package commonist.task.upload

import java.io._
import java.net._

import net.psammead.minibpp.Compiler
import bsh.Interpreter

import scutil.base.implicits._
import scutil.log._

import commonist.data._
import commonist.util._

/** compiles image metadata into a <code>[[Template:Information]]</code> for commons or something similar for other wikis */
final class UploadTemplates(loader:Loader, wiki:WikiData) extends Logging {
	/** edit summary for writing a gallery */
	def gallerySummary(version:String, failureCount:Int):String =
			"commonist " + version + ((failureCount != 0) cata ("", show", ${failureCount.toString} errors"))
	
	/** compiles into wikitext */
	def galleryDescription(common:Common, batch:Batch):String =
			template("gallery", Map(
				"common"	-> common,
				"batch"		-> batch
			))
	
	/** compiles an image description into wikitext */
	def imageDescription(common:Common, upload:Upload):String =
			template("image", Map(
				"common"	-> common,
				"upload"	-> upload
			))
	
	private def template(typ:String, data:Map[String,AnyRef]):String = {
		val specific	= typ + "_" + wiki.family + (wiki.site cata ("", "_" + _)) + ".bpp"
		val generic		= typ + "_default.bpp"
		val url			=	(loader resourceURL specific)	orElse
							(loader resourceURL generic)	getOrError
							(show"neither specific template: ${specific} nor generic template: ${generic} could be found")
		try {
			compile(url, data) |> TextUtil2.restrictEmptyLines |> TextUtil2.trimLF
		}
		catch { case e:Exception =>
			ERROR("exception occurred while using template", url, e)
			throw e
		}
	}
	
	private val TEMPLATE_ENCODING	= "UTF-8"
	
	private def compile(url:URL, data:Map[String,AnyRef] ):String = {
		val code = new InputStreamReader(url.openConnection.getInputStream, TEMPLATE_ENCODING) use { tin =>
			new Compiler() compile tin
		}

		val sout	= new StringWriter
		val xout	= new PrintWriter(sout)
		
		val interpreter	= new Interpreter()
		interpreter set ("out",	xout)
		data.foreach { case (key,value) => interpreter set (key, value) }

		val cin	= new StringReader(code)
		interpreter eval (cin, interpreter.getNameSpace, url.toExternalForm)
		
		sout.toString
	}
}
