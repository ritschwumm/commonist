package commonist.mediawiki

import java.io.File

import scutil.core.implicits._
import scutil.log._

import scjson.ast._
import scjson.codec._
import scjson.ast.JsonNavigation._

final class API(apiURL:String) extends Logging {
	// BETTER get rid of this
	private implicit class AddOnly(value:Option[JsonValue])	{
		def only:Option[JsonValue] =
			value.flatMap(
				_.cata[Option[JsonValue]](
					onNull		= () => None,
					onBoolean	= _ => None,
					onNumber	= _ => None,
					onString	= _ => None,
					onArray		= _.headOption,
					onObject	= _.headOption.map(_._2)
				)
			)
	}

	val connection	= new Connection(apiURL)

	//------------------------------------------------------------------------------

	/** login a user with a given password */
	def login(user:String, password:String):LoginResult = {
		val	req1	=
			Seq(
				"action"		-> "login",
				"format"		-> "json",
				"lgname"		-> user,
				"lgpassword"	-> password
			)
		val res1	= connection POST req1
		require(res1.nonEmpty, "no json result")
		errorCode(res1) match {
			case Some(code)	=> return LoginResult.Error(code)
			case None		=>
		}

		val login	= res1	/ "login"
		val token	= (login	/ "token").string
		val outUser	= (login	/ "lgusername").string

		resultCode(login) match {
			case Some("NeedToken")	=>	// handled later
			case Some("Success")	=> return LoginResult.Success(outUser getOrError "expected a username")
			case Some(code)			=> return LoginResult.Failure(code)
			case None				=> sys error ("expected a result")
		}

		val req2	=
			req1 ++
			optionally("lgtoken" -> token)
		val res2	= connection POST req2
		require(res2.nonEmpty,	"no json result")
		errorCode(res2)  match {
			case Some(code)	=> return LoginResult.Error(code)
			case None		=>
		}

		val login2		= res2		/ "login"
		val outUser2	= (login2	/ "lgusername").string

		resultCode(login2) match {
			case Some("Success")	=> LoginResult.Success(outUser2 getOrError "expected a username")
			case Some(code)			=> LoginResult.Failure(code)
			case _					=> sys error ("expected a result")
		}
	}

	/** logout a user */
	def logout():Unit	= {
		val	req	=
			Seq(
				"action"	-> "logout",
				"format"	-> "json"
			)
		val res	= connection POST req
		require(res.nonEmpty,	"no json result")
	}

	/** edit a page with an editor function, if it returns None editing is aborted */
	def edit(title:String, summary:String, section:Option[Int], change:String=>Option[String]):EditResult = {
		val sectionString	= section map {_.toString}

		val	req1	=
			Seq(
				"action"	-> "query",
				"format"	-> "json",
				"prop"		-> "info|revisions",
				"intoken"	-> "edit",	// provides edittoken and starttimestamp
				"rvprop"	-> "timestamp|content",
				"titles"	-> title
			) ++
			optionally(
				"rvsection"	-> sectionString
			)
		val res1			= connection POST req1
		require(res1.nonEmpty,	"no json result")
		errorCode(res1) match {
			case Some(code)	=> return EditResult.Error(code)
			case None		=>
		}

		val page			= (res1 	/ "query" / "pages").only
		val edittoken		= (page 	/ "edittoken").string
		val starttimestamp	= (page		/ "starttimestamp").string
		val revision		= (page		/ "revisions").only
		val basetimestamp	= (revision	/ "timestamp").string
		val content			= (revision	/ "*").string
		val missing			= (page		/ "missing").string

		val original	= content orElse (missing map { _ => "" })
		val changed		= original flatMap change
		if (changed.isEmpty)	return EditResult.Aborted
		val changed1	= changed getOrError "no text???"

		val	req2	=
			Seq(
				"action"	-> "edit",
				"format"	-> "json",
				"title"		-> title,
				"summary"	-> summary,
				"text"		-> changed1
			) ++
			optionally(
				"token"				-> edittoken,
				"basetimestamp"		-> basetimestamp,
				"starttimestamp"	-> starttimestamp,
				"section"			-> sectionString
			)
		val res2	= connection POST req2
		require(res2.nonEmpty,	"no json result")
		errorCode(res2) match {
			case Some(code)	=> return EditResult.Error(code)
			case None		=>
		}

		// TODO handle edit conflicts
		val edit		= res2 / "edit"
		val outTitle	= (edit / "title").string

		resultCode(edit) match {
			case Some("Success")	=> EditResult.Success(outTitle getOrError "expected a title")
			case Some(code)			=> EditResult.Failure(code)
			case _					=> sys error ("expected a result")
		}
	}

	/** upload a file */
	def upload(filename:String, summary:String, text:String, file:File, callback:UploadCallback):UploadResult = {
		val	req1	=
			Seq(
				"action"	-> "query",
				"format"	-> "json",
				"prop"		-> "info",
				"intoken"	-> "edit",
				"titles"	-> (Namespace file filename)
			)

		val res1			= connection POST req1
		require(res1.nonEmpty,	"no json result")
		errorCode(res1) match {
			case Some(code)	=> return UploadResult.Error(code)
			case None		=>
		}

		val page			= (res1 / "query" / "pages").only
		val edittoken		= (page / "edittoken").string

		val req2	=
			Seq(
				"action"	-> "upload",
				"format"	-> "json",
				"filename"	-> filename,
				"comment"	-> summary,
				"text"		-> text
				//	ignorewarnings	url	sessionkey
			) ++
			optionally(
				"token"	-> edittoken
			)

		// NOTE either 'sessionkey', 'file', 'url'
		val res2	= connection.POST_multipart(req2, "file", file, callback.progress)
		require(res2.nonEmpty,	"no json result")
		errorCode(res2) match {
			case Some(code)	=> return UploadResult.Error(code)
			case None		=>
		}

		val upload		= res2		/ "upload"
		val outName		= (upload	/ "filename").string
		val sessionkey	= (upload	/ "sessionkey").string
		val warnings	= upload	/ "warnings"

		resultCode(upload) match {
			case Some("Success")	=>
				val	name	= outName getOrError "expected filename"
				val	title	= Namespace file name
				return UploadResult.Success(name, title)
			case Some("Warning")	=> // handled later
			case Some(code)			=> return UploadResult.Failure(code)
			case _					=> sys error ("expected a result")
		}

		// TODO handle more warnings with messages
		val warningWasDeleted		= (warnings / "was-deleted").string			map UploadWarning.WasDeleted.apply
		val warningExists			= (warnings / "exists").string				map	UploadWarning.Exists.apply
		val warningDuplicate		= (warnings / "duplicate").arraySeq			map { _ mapFilter { _.string } } map UploadWarning.Duplicate.apply
		val warningDuplicateArchive	= (warnings / "duplicate-archive").string	map UploadWarning.DuplicateArchive.apply
		val ignorableWarnings:Set[UploadWarning]	= Set(warningWasDeleted, warningExists, warningDuplicate, warningDuplicateArchive).flattenOption
		if (ignorableWarnings.nonEmpty && !(callback ignore ignorableWarnings))	 return UploadResult.Aborted(ignorableWarnings)

		// handle other warnings
		val ignoredWarningKeys			= Set("was-deleted", "exists",	"duplicate", "duplicate-archive", "large-file")
		val allWarningKeys:Set[String]	= (warnings.toMap map { _.keys.toSet }).toSet.flatten
		val relevantWarnings			= allWarningKeys -- ignoredWarningKeys
		if (relevantWarnings.nonEmpty)	return UploadResult.Failure(relevantWarnings mkString ", ")

		require(sessionkey.isDefined, "to resume after warnings, a sessionkey is required")

		val req3	=
			Seq(
				"action"			-> "upload",
				"format"			-> "json",
				"filename"			-> filename,
				"comment"			-> summary,
				"text"				-> text,
				"ignorewarnings"	-> "true"
				// url
			) ++
			optionally(
				"token"			-> edittoken,
				"sessionkey"	-> (sessionkey map { _.toString })
			)

		val res3	= connection POST req3
		require(res3.nonEmpty,	"no json result")
		errorCode(res3) match {
			case Some(code)	=> return UploadResult.Error(code)
			case None		=>
		}

		val upload2		= res3		/ "upload"
		val outName2	= (upload2	/ "filename").string

		resultCode(upload2) match {
			case Some("Success")	=>
				val	name	= outName2 getOrError "expected filename"
				val	title	= Namespace file name
				// NOTE api.php does not write the description page if it's not the initial upload
				if (warningExists.isDefined) {
					val	editResult	= edit(title, "overwritten", None, Function const Some(text))
					editResult match {
						case EditResult.Failure(code) =>
							ERROR("could not change overwritten file description (failure)", title, code)
						case EditResult.Error(code) =>
							ERROR("could not change overwritten file description (error)", title, code)
						case _ =>
							// ok
					}
				}
				return UploadResult.Success(name, title)
			case Some(code)			=> return UploadResult.Failure(code)
			case _					=> sys error ("expected a result")
		}
	}

	//------------------------------------------------------------------------------

	private def resultCode(response:Option[JsonValue]):Option[String] =
		(response / "result").string

	private def errorCode(response:Option[JsonValue]):Option[String] = {
		val	error	= response / "error"
		error foreach { it => ERROR(JsonCodec encodeShort it) }
		(error / "code").string
	}

	/** helper function for optional request parameters */
	private def optionally(values:(String,Option[String])*):List[(String,String)] =
		values.toList mapFilter optionally1

	// NOTE this is some kind of sequence (traversable)
	private def optionally1(value:(String,Option[String])):Option[(String,String)] =
		value match {
			case (key, Some(value))	=> Some((key, value))
			case (key, None)		=> None
		}
}
