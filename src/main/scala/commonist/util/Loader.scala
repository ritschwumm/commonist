package commonist.util

import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL

import scutil.Implicits._
import scutil.log.Logging

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
			new File(directory, path).guardExists map { _.toURI.toURL }
			
	def classloaderURL(resourcePrefix:String, path:String):Option[URL] =
			getClass resourceOption (resourcePrefix + path)
}
