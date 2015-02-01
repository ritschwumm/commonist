package commonist.task

import java.io.File
import java.nio.charset.Charset
import java.util.{ List => JUList }
import javax.swing.JOptionPane

import scutil.lang.ISeq
import scutil.implicits._
import scutil.io.Charsets
import scutil.gui.SwingUtil._
import scutil.log._

import scmw._

import commonist._
import commonist.data._
import commonist.task.upload._
import commonist.ui._
import commonist.ui.later._
import commonist.util._

/** upload files selected in the ImageListUI */
final class UploadFilesTask(
	settingsDir:File,
	loader:Loader, 
	commonData:CommonData, 
	imageListData:ImageListData, 
	mainWindow:MainWindow,
	imageListUI:ImageListUI,
	statusUI:StatusUI 
) extends Task {
	private val statusUILater		= new StatusUILater(statusUI)
	private val imageListUILater	= new ImageListUILater(imageListUI)
			
	private val	wiki			= commonData.wiki
	private val	api				= new API(wiki.api, Constants.ENABLE_API_WRITE)
	private val wikiName		= wiki.toString
	private val uploadTemplates	= new UploadTemplates(loader, wiki)
	
	override protected def execute() {
		if (!imageListData.hasSelected) {
			INFO("nothing to upload")
			statusUILater halt ("status.upload.empty")
			return
		}
		
		try {
			if (!login())	return
			val common	= Common(
				commonData.description.trim,
				commonData.date.trim,
				commonData.source.trim,
				commonData.author.trim,
				commonData.permission.trim,
				commonData.license.template,
				commonData.license.description,
				Parser parseCategories commonData.categories
			)
			val	uploads	= upload(common)
			if (Constants.ENABLE_GALLERY) {
				gallery(common, uploads)
			}
			INFO("upload finished")
		}
		catch { 
			case e:AbortedException =>
				ERROR("upload task aborted")
				statusUILater halt ("status.upload.aborted")
			case e:Exception =>
				ERROR("upload task error", e)
				// TODO hack
				// status.upload.error=Hochladen von {0} fehlgeschlagen ({1})
				statusUILater halt ("status.upload.error", "", e.getMessage)
		}
	}
	
	/*
	statusUILater halt ("status.login.successful", wikiName)
	statusUILater halt ("status.login.aborted")
	statusUILater halt ("status.login.error", wikiName, e.getMessage)
	statusUILater halt ("status.login.wrongpw", wikiName)
	*/
	private def login():Boolean = {
		INFO("logging in")
		statusUILater indeterminate ("status.login.started", wikiName)
		check()
			
		val loginResult	= api login (commonData.user.trim, commonData.password)
		loginResult match {
			case LoginSuccess(userName)	=>
				INFO("login successful", userName)
				statusUILater halt ("status.login.successful", wikiName)
				true
			case LoginFailure(code)	=>
				INFO("login failed", code)
				// TODO more detail
				statusUILater halt ("status.login.wrongpw", wikiName)
				false
			case LoginError(code)	=>
				INFO("login error", code)
				statusUILater halt ("status.login.error", wikiName, code)
				false
		}
	}
	
	/*
	statusUILater halt ("status.upload.aborted")
	statusUILater halt ("status.upload.error", path, e.getMessage)
	*/
	private def upload(common:Common):ISeq[Upload] = {
		INFO("uploading files")
		
		// TODO normalizeTitle(FileName.fix( is stupid
		val	selected	= imageListData.selected
		def titleAt(index:Int):String	=
					 if (index < 0)					null
				else if (index >= selected.size)	null
				else selected(index).name |> Filename.fix |> Filename.normalizeTitle |> Namespace.file
				
		selected.zipWithIndex map { case (imageData, index) => 
			check()
			
			val	file		= imageData.file
			val fileLength	= file.length
			val fileName	= file.getName
			val filePath	= file.getPath
			val	name		= Filename.normalizeTitle(Filename.fix(imageData.name))
			val title		= Namespace.file(name)
			val previous	= titleAt(index-1)
			val next		= titleAt(index+1)
			val coords		= imageData.coordinates
			val coordParts	= Parser parseCoordinates imageData.coordinates
			val latitude	= coordParts map { _._1 } orNull;
			val longitude	= coordParts map { _._2 } orNull;
			val categories	= Parser parseCategories imageData.categories
			
			val upload	= Upload(
				name,
				title,
				null,
				previous,
				next,
				imageData.description.trim,
				imageData.date.trim,
				categories,
				coords.trim,
				latitude,
				longitude
			)
			
			statusUILater halt ("status.upload.started", fileName)
			val callback	= new MyUploadCallback(mainWindow, statusUILater, fileLength, fileName, name)
			val text		= uploadTemplates imageDescription (common, upload)
			val	watch		= true
			
			val uploaded	= api upload (name, "", text, watch, file, callback)
			uploaded match {
				case UploadSuccess(fileName, pageTitle)	=> 
					INFO("upload successful", fileName, pageTitle)
					statusUILater halt ("status.upload.successful", fileName, pageTitle)
					imageListUILater uploadFinished (file, true)
					upload copy (name=fileName, title=pageTitle)
				case UploadAborted(warnings)	=>
					ERROR("upload aborted", fileName)
					statusUILater halt ("status.upload.error", fileName, "aborted")
					imageListUILater uploadFinished (file, false)
					// TODO just remove it from the list?
					// TODO more detail
					upload copy (error=s"aborted: ${renderWarnings(warnings)}")
				case UploadFailure(code)	=>
					// TODO more detail
					ERROR("upload failed", fileName, code)
					statusUILater halt ("status.upload.error", fileName, code)
					imageListUILater uploadFinished (file, false)
					upload copy (error=code)
				case UploadError(code)	=>
					ERROR("upload error", fileName, code)
					statusUILater halt ("status.upload.error", fileName, code)
					imageListUILater uploadFinished (file, false)
					upload copy (error=code)
			}
		}
	}
	
	private def renderWarnings(warnings:Set[UploadWarning]):String	= 
			warnings map renderWarning mkString ", "
	
	private def renderWarning(warning:UploadWarning):String	= warning match {
		case UploadWarningWasDeleted(name)			=> renderWarning("was-deleted", name)
		case UploadWarningExists(name)				=> renderWarning("exists", name)
		case UploadWarningDuplicate(names)			=> names map { renderWarning("duplicate", _) } mkString ", "
		case UploadWarningDuplicateArchive(name)	=> renderWarning("duplicate-archive", name)
	}
	
	private def renderWarning(key:String, conflict:String):String	=
			key + ": " + renderFile(conflict)
	
	private def renderFile(name:String):String	= 
			"[[:" + (Namespace file name) + "]]"
		
	private def renderLink(s:String):String	=
			"[[" + s + "]]"
	
	/*
	statusUILater halt ("status.gallery.error", e.getMessage)
	statusUILater halt ("status.gallery.editConflict",	"[[" + title + "]]")
	*/
	def gallery(common:Common, uploads:ISeq[Upload]) = {
		INFO("changing gallery")
		check()
		
		val	title	= Namespace user (commonData.user + "/gallery")
		val (sucesses, failures)	= uploads partition { _.error == null }
		
		val	batch	= Batch(
			uploads.toJList,
			sucesses.toJList,
			failures.toJList
		)
		val summary	= uploadTemplates gallerySummary		(commonist.BuildInfo.version, failures.size)
		val text	= uploadTemplates galleryDescription	(common, batch)
		
		// backup gallery text
		val backup	= settingsDir / "gallery.txt"
		INFO("writing gallery", backup)
		backup writeString (Charsets.utf_8, text)
		
		statusUILater indeterminate ("status.gallery.loading", renderLink(title))
		val editResult	= api edit (title, summary, None, { oldText =>
			statusUILater indeterminate ("status.gallery.storing", renderLink(title))
			val newText	= text + "\n\n" + (TextUtil2 trimLF oldText)
			Some(newText)
		})
		editResult match {
			case EditSuccess(pageTitle)	=> 
				statusUILater halt ("status.gallery.updated",	renderLink(title))
			case EditAborted		=>
				// will not happen
			case EditFailure(code)	=> 
				// TODO more detail
				statusUILater halt ("status.gallery.error",		code + " in " + renderLink(title))
			case EditError(code)	=> 
				statusUILater halt ("status.gallery.error",		code + " in " + renderLink(title))
		}
	}
	
	/*
	statusUILater halt ("status.logout.error", wikiName, e.getMessage)
	if (!success) { statusUILater halt ("status.logout.failed", wikiName); return }
	*/
	private def logout() {
		INFO("logging out")
		statusUILater indeterminate ("status.logout.started", wiki)
		api.logout()
		statusUILater halt ("status.logout.successful", wikiName)
	}
	
	//==============================================================================
	
	/** asks the user when somwething about a file upload is unclear */
	private class MyUploadCallback(mainWindow:MainWindow, statusUILater:StatusUILater, fileLength:Long, fileName:String, name:String) extends UploadCallback {
		def progress(bytes:Long) {
			// System.err.println("written " + bytes + " of " + ofBytes)
			val percent	= (bytes * 100 / fileLength).toInt
			statusUILater determinate ("status.upload.progress", percent, 100, fileName, int2Integer(percent))
			// rate = (bytes - oldBytes) / (time  - oldTime)
		}
		
		/** ask the user a yes/no message */
		def ignore(problems:Set[UploadWarning]):Boolean	= {
			require(problems.nonEmpty,	"asking the user about no problem is problematic")
			
			val title	= Messages message ("query.upload.title", name)
			
			val content	= problems 
					.map {
						case UploadWarningExists(conflict)				=> (0, "query.upload.ignoreFileexists.message",			conflict)
						case UploadWarningWasDeleted(conflict)			=> (1, "query.upload.ignoreFilewasdeleted.message",		conflict)
						case UploadWarningDuplicate(conflicts)			=> (2, "query.upload.ignoreDuplicate.message",			conflicts mkString ", ")
						case UploadWarningDuplicateArchive(conflict)	=> (3, "query.upload.ignoreDuplicateArchive.message",	conflict)
					}
					.toVector
					.sortBy	{ case (prio, _, _)			=> prio	}
					.map	{ case (_, key, conflict)	=> Messages message (key, name, conflict)	}
					.mkString ("\n")
			
			val body	= Messages message ("query.upload.body", name, content)
			
			try {
				edtWait {
					JOptionPane.YES_OPTION == 
					(JOptionPane showConfirmDialog (mainWindow.window, body, title, JOptionPane.YES_NO_OPTION))
				}
			}
			catch { case e:Exception =>
				ERROR("callback error", e)
				throw e
			}
		}
	}
}
