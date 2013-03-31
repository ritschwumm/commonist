package commonist.util

import java.io._
import java.net.URL
import java.text.MessageFormat
import java.util.Properties

import scala.collection.JavaConversions._

import scutil.Implicits._
import scutil.Resource._
import scutil.log._

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
	val defaultProps	= load(defaultURL)
	val userLangProps	= userLangURL map load _ getOrElse Map.empty

	def getText(key:String):String = get(key)
	
	def getMessage(key:String, args:Object*):String	= 
			try {
				MessageFormat format (get(key), args.map(_.asInstanceOf[AnyRef]) : _*)
			}
			catch {
				case e:Exception	=>
					ERROR("message cannot be used: " + key)
					throw e
			}
	
	private def get(key:String):String =
			(userLangProps get key)	orElse
			(defaultProps get key)	getOrError
			("message not available: " + key)
	
	private def load(url:URL):Map[String,String] =
			url.openStream() use { in =>
				val props	= new Properties()
				props load in
				Map() ++ props
			}
}
