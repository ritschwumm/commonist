package commonist.task

import java.io.File

import scala.language.postfixOps
import scala.xml._

import scutil.base.implicits._
import scutil.core.implicits._
import scutil.io._
import scutil.log._

import commonist._
import commonist.thumb._
import commonist.ui._
import commonist.ui.later._
import commonist.util._

/** change the directory displayed in the ImageListUI */
final class ChangeDirectoryTask(mainWindow:MainWindow, imageListUI:ImageListUI, statusUI:StatusUI, thumbnails:Thumbnails, directory:File, loader:Loader) extends Task {
	private val imageListUILater	= new ImageListUILater(imageListUI)
	private val statusUILater		= new StatusUILater(statusUI)

	private def getOaiPmhProps():Map[String,String] = {
		val propsURL = loader resourceURL "oaipmh.properties" getOrError "cannot load oaipmh.properties"
		PropertiesUtil loadURL (propsURL, None)
	}

	override protected def execute() {
		DEBUG("clear")

		imageListUILater.clear()
		Thread.`yield`()	//Thread.sleep(50)

		DEBUG("listFiles")
		val listed	= directory
				.childrenWhere	{ file:File => file.isFile && !file.isHidden }
				.getOrElse		{ WARN("directory does not exist", directory); return }

		val sorted	= listed sortBy { _.getPath }

		val	(readable,unreadable)	= sorted partition { _.canRead }
		unreadable foreach { it => WARN("cannot read", it) }

		val (xmls,images) = readable partition { f => f.getName() endsWith ".xml" }
		val oaipmh = xmls.map(XML loadFile).filter("OAI-PMH" == _.label).map(new OaiPmh2(_, getOaiPmhProps)).toVector

		val max		= images.length
		var cur		= 0
		var last	= 0L
		try {
			for (file <- images) {
				check()

				statusUILater determinate ("imageList.loading", cur, max, file.getPath, int2Integer(cur), int2Integer(max))
				cur	= cur + 1

				// using Thread.interrupt while this is running kills the EDT??
				val thumbnail			= thumbnails thumbnail file
				val thumbnailMaxSize	= thumbnails.getMaxSize
				imageListUILater add (file, oaipmh, thumbnail, thumbnailMaxSize)
				try { Thread.sleep(100) }
				catch { case e:InterruptedException => WARN("interrupted", e) }

				// update when a given number of ImageUIs have been added
				// or a given delay has elapsed or
				val now	= System.currentTimeMillis
				if (now - last > Constants.IMAGELIST_UPDATE_DELAY
				|| (cur % Constants.IMAGELIST_UPDATE_COUNT) == 0) {
					imageListUILater.updateSelectStatus()
					// this doesn't have to run in the EDT,
					// but is needed to make our changes visible
					mainWindow.revalidate()
					last	= now
				}
			}

			statusUILater halt ("imageList.loaded", directory.getPath, int2Integer(max))
		}
		catch { case e:AbortedException =>
			INFO("loading image list aborted")
			// TODO update statusUI?
		}
		imageListUILater.updateSelectStatus()
	}
}
