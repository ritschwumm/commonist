package commonist.util

import java.io._

import scala.collection.mutable
import scala.collection.JavaConverters._

import scutil.Implicits._
import scutil.log._
import scutil.io._

/** encapsulates a properties file */
class Settings(file:File) extends Logging {
	private var propertiesMap:Map[String,String]	= Map.empty
	
	/** get a property */
	def get(name:String):Option[String] = propertiesMap get name
	
	/** get a property or its default value */
	def getOrElse(name:String, defaultValue:String):String = get(name) getOrElse defaultValue
	
	/** set a property */
	def set(name:String, value:String) {
		propertiesMap	= propertiesMap updated (name, value)
	}
	
	def getInt(name:String):Option[Int]	=
			get(name) map { Integer.parseInt(_) }
		
	def getIntOrElse(name:String, defaultValue:Int):Int	=
			getInt(name) getOrElse defaultValue
		
	def setInt(name:String, value:Int) { 
		set(name, value.toString) 
	}
	
	/** loads our properties file */
	def load() {
		if (file.exists) {
			propertiesMap = PropertiesUtil loadFile file
		}
		else {
			INFO("setting file does not exist: " + file.getPath)
		}
	}

	/** saves our properties file */
	def save() {
		PropertiesUtil saveFile (file, propertiesMap)
	}
}
