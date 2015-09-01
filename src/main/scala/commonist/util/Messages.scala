package commonist.util

import java.net.URL
import java.text.MessageFormat
import java.util.Properties

import scutil.implicits._
import scutil.log._
import scutil.io._

// TODO ugly
object Messages {
	var SELF:Messages	= null
	
	def init(defaultURL:URL, userLangURL:Option[URL]) {
		SELF	= new Messages(defaultURL, userLangURL)
	}
	
	def text(key:String):String 					= SELF.getText(key)
	def message(key:String, args:Object*):String	= SELF.getMessage(key, args:_*)
}
	
/** encapsulates user messages loaded from a properties document */
class Messages(defaultURL:URL, userLangURL:Option[URL]) extends Logging {
	val defaultProps	= PropertiesUtil loadURL defaultURL
	val userLangProps	= userLangURL cata (Map.empty[String,String], PropertiesUtil.loadURL)

	def getText(key:String):String = get(key)
	
	def getMessage(key:String, args:Object*):String	=
			try {
				MessageFormat format (get(key), args.map(_.asInstanceOf[AnyRef]) : _*)
			}
			catch { case e:Exception	=>
				ERROR(s"message cannot be used: ${key}")
				throw e
			}
	
	private def get(key:String):String =
			(userLangProps get key)	orElse
			(defaultProps get key)	getOrError
			(s"message not available: ${key}")
}
