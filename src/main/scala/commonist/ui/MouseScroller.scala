package commonist.ui

import java.awt.{ List => AwtList, _ }
import java.awt.event._
import javax.swing._
import javax.swing.event._

import commonist.util._

/**
 * moves a JComponent withing a JViewport with the mouse
 * usage: add an instance as MouseListener and MouseMotionListener to the target componente
 */
final class MouseScroller(picture:JComponent) extends MouseInputAdapter {
	private var x	= 0
	private var y	= 0
	
	//def mouseClicked(ev:MouseEvent) {}
	
	override def mousePressed(ev:MouseEvent) {
		x	= ev.getX
		y	= ev.getY
	}
	
	//def mouseReleased(ev:MouseEvent) {}
	//def mouseEntered(ev:MouseEvent) {}
	//def mouseExited(ev:MouseEvent) {}
	
	override def mouseDragged(ev:MouseEvent):Unit	=
			this.picture.getParent match {
				case parent:JViewport =>
					val viewPort	= parent.asInstanceOf[JViewport]
					
					val full	= this.picture.getSize()
					val extent	= viewPort.getExtentSize
					val pos		= viewPort.getViewPosition
					
					pos translate (x - ev.getX, y - ev.getY)
					
					val posLimits	=
							new Dimension(
								full.width  - extent.width,
								full.height - extent.height
							)
					val posLimited	= UIUtil2 limitToBounds (pos, posLimits)
			
					viewPort setViewPosition posLimited
					
				case _ =>
			}
	
	//def mouseMoved(ev:MouseEvent) {}
}
