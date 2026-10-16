package commonist.ui.later

import java.io.File

import javax.swing.Icon

import scutil.gui.SwingUtil._

import commonist.ui.ImageListUI

/** wraps a ImageListUI's methods in SwingUtilities.invokeAndWait */
final class ImageListUILater(ui:ImageListUI) {
	def clear():Unit	=
		edt {
			ui.clear()
		}()

	def add(file:File, thumbnail:Option[Icon], thumbnailMaxSize:Int):Unit	=
		edt {
			ui.add(file, thumbnail, thumbnailMaxSize)
		}()

	def updateSelectStatus():Unit	=
		edt {
			ui .updateSelectStatus ()
		}()

	def uploadFinished(file:File, success:Boolean):Unit	=
		edt {
			ui.uploadFinished(file, success)
		}()
}
