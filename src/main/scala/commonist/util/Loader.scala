package commonist.util

import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL

import scutil.log.Logging
import scutil.ext.AnyRefImplicits._
import scutil.ext.OptionImplicits._
import scutil.ext.FileImplicits._

/** loads resources from a set of URL-paths */
final class Loader(settingsDir:File, etcDir:File, resourcesDir:File, resourcePrefix:String) extends Logging {
	def resourceURL(path:String):Option[URL] = 
			directoryURL(settingsDir, path) 		orElse 
			directoryURL(etcDir, path)				orElse
			directoryURL(resourcesDir, path)		orElse
			classloaderURL(resourcePrefix, path)	noneEffect
			{ DEBUG(
					"failed to access resource", path, "tried to find in",
					settingsDir, etcDir, resourcesDir, "classpath:" + resourcePrefix)
			}
	
	def directoryURL(directory:File, path:String):Option[URL] =
			new File(directory, path).existsOption map { _.toURI.toURL }
			
	def classloaderURL(resourcePrefix:String, path:String):Option[URL] =
			getClass getResource (resourcePrefix + path) nullOption
}
