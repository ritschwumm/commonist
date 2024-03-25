package commonist.ui.later

import java.io.File

import javax.swing.Icon

import scutil.gui.SwingUtil._

import commonist.ui.ImageListUI
import commonist.util.OaiPmh2

/** wraps a ImageListUI's methods in SwingUtilities.invokeAndWait */
final class ImageListUILater(ui:ImageListUI) {
	def clear() {
		edtWait {
			ui.clear()
		}
	}

	def add(file:File, oaipmh:Vector[OaiPmh2], thumbnail:Option[Icon], thumbnailMaxSize:Int) {
		edtWait {
			ui add (file, oaipmh, thumbnail, thumbnailMaxSize)
		}
	}

	def updateSelectStatus() {
		edtWait {
			ui updateSelectStatus ()
		}
	}

	def uploadFinished(file:File, success:Boolean) {
		edtWait {
			ui uploadFinished (file, success)
		}
	}

}
