package commonist.util

import java.io._
import java.net._

import scutil.base.implicits._
import scutil.core.implicits._
import scutil.log._

/** loads resources from a set of URL-paths */
final class Loader(settingsDir:File, etcDir:File, resourcesDir:File, resourcePrefix:String) extends Logging {
	def resourceURL(path:String):Option[URL] =
			directoryURL(settingsDir, path) 		orElse
			directoryURL(etcDir, path)				orElse
			directoryURL(resourcesDir, path)		orElse
			classloaderURL(resourcePrefix, path)	noneEffect {
				DEBUG(
					"failed to access resource", path, "tried to find in",
					settingsDir, etcDir, resourcesDir, so"classpath:${resourcePrefix}"
				)
			}
	
	private def directoryURL(directory:File, path:String):Option[URL] =
			new File(directory, path).guardExists map { _.toURI.toURL }
			
	private def classloaderURL(resourcePrefix:String, path:String):Option[URL] =
			getClass.resources findUrl (resourcePrefix + path)
}
