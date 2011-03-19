package commonist.ui.later

import java.io.File

import javax.swing.Icon

import scutil.gui.SwingUtil._

import commonist.ui.ImageListUI

/** wraps a ImageListUI's methods in SwingUtilities.invokeAndWait */
final class ImageListUILater(ui:ImageListUI) {
	def clear() {
		edtWait { 
			ui.clear() 
		} 
	}
	
	def add(file:File, thumbnail:Option[Icon], thumbnailMaxSize:Int) {
		edtWait { 
			ui add (file, thumbnail, thumbnailMaxSize) 
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
