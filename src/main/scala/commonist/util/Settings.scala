package commonist.util

import java.io._

import scala.collection.JavaConversions._

import scutil.Implicits._
import scutil.log.Logging

/** encapsulates a properties file */
class Settings(file:File) extends Logging {
	private val propertiesRaw	= new java.util.Properties
	private val propertiesMap:scala.collection.mutable.Map[String,String]	= propertiesRaw
	
	/** get a property */
	def get(name:String):Option[String] = propertiesMap get name
	
	/** get a property or its default value */
	def get(name:String, defaultValue:String):String = get(name) getOrElse defaultValue
	
	/** set a property */
	def set(name:String, value:String) {
		propertiesMap update (name, value)
	}
	
	def getInt(name:String):Option[Int] 			= get(name) map { Integer.parseInt(_) }
	def getInt(name:String, defaultValue:Int):Int	= getInt(name) getOrElse defaultValue
	def setInt(name:String, value:Int) { set(name, ""+value) }
	
	/** loads our properties file */
	def load() {
		if (!file.exists())	{ INFO("setting file does not exist: " + file.getPath); return }
		file withInputStream { in =>
			propertiesRaw.load(in)
		}
	}

	/** saves our properties file */
	def save() {
		file withOutputStream { out =>
			propertiesRaw.store(out, "the commonist")
		}
	}
}
