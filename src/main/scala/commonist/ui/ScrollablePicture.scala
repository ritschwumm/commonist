package commonist.ui

import java.awt.{ List => _, _ }
import javax.swing._

import commonist.Constants

/** a JLabel used to display an image in fullsize */
final class ScrollablePicture extends JLabel with Scrollable {
	setAutoscrolls(true)
	
	val mouse	= new MouseScroller(this)
	addMouseListener(mouse)
	addMouseMotionListener(mouse)

    def getScrollableUnitIncrement(visibleRect:Rectangle, orientation:Int, direction:Int):Int = {
    	val currentPosition	= if (orientation == SwingConstants.HORIZONTAL)	visibleRect.x else visibleRect.y
        if (direction < 0) {
        	val newPosition = currentPosition - (currentPosition / Constants.FULLSIZE_MAX_UNIT_INCREMENT) * Constants.FULLSIZE_MAX_UNIT_INCREMENT
            if (newPosition == 0) Constants.FULLSIZE_MAX_UNIT_INCREMENT else newPosition
        }
		else {
            ((currentPosition / Constants.FULLSIZE_MAX_UNIT_INCREMENT) + 1) * Constants.FULLSIZE_MAX_UNIT_INCREMENT - currentPosition
        }
    }

    def getScrollableBlockIncrement(visibleRect:Rectangle, orientation:Int, direction:Int):Int = {
		if (orientation == SwingConstants.HORIZONTAL) {
			visibleRect.width  - Constants.FULLSIZE_MAX_UNIT_INCREMENT
		}
		else {
			visibleRect.height - Constants.FULLSIZE_MAX_UNIT_INCREMENT
		}
    }

    def getPreferredScrollableViewportSize():Dimension = getPreferredSize
    def getScrollableTracksViewportWidth():Boolean	= false
    def getScrollableTracksViewportHeight():Boolean	= false

    /*
    private class MouseBorderScroll extends MouseInputAdapter {
    	def mouseDragged(MouseEvent ev) {
    		Rectangle r = new Rectangle(ev.getX, ev.getY, 1, 1)
    		scrollRectToVisible(r)
    	}
    }
    */
}
