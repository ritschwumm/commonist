package commonist.util

import java.awt.event._
import java.beans._
import javax.swing._
import javax.swing.event._
import javax.swing.text._
import javax.swing.undo._

import scutil.log._

/** adds undo to a {@link JTextComponent} */
trait TextComponentUndo extends Logging { self:JTextComponent =>
	private val REDO_KEY_STROKE	= KeyStroke.getKeyStroke("control Y")
	private val UNDO_KEY_STROKE	= KeyStroke.getKeyStroke("control Z")
	private val REDO_ACTION		= "Redo"
	private val UNDO_ACTION		= "Undo"
	private val LIMIT			= 100
	
	private val undoManager	= new UndoManager
	undoManager setLimit LIMIT
	
	private val propertyListener	= new PropertyChangeListener {
		def propertyChange(ev:PropertyChangeEvent) {
			if (ev.getPropertyName != "document")	return
			val oldDocument	= ev.getOldValue.asInstanceOf[Document]
			val newDocument	= ev.getNewValue.asInstanceOf[Document]
			oldDocument	removeUndoableEditListener	undoableEditListener
			newDocument	addUndoableEditListener		undoableEditListener
		}
	}
		
	private val undoableEditListener = new UndoableEditListener() {
		def undoableEditHappened(ev:UndoableEditEvent) {
			undoManager addEdit ev.getEdit
		}
	}

	private val undoAction = new AbstractAction {
		def actionPerformed(ev:ActionEvent) {
			try {
				if (undoManager.canUndo) {
					undoManager.undo()
				}
			} 
			catch {
				case e:CannotUndoException 	=>
					ERROR("undo failed", e)
			}
		}
	}

	private val redoAction = new AbstractAction {
		def actionPerformed(ev:ActionEvent) {
			try {
				if (undoManager.canRedo) {
					undoManager.redo()
				}
			}
			catch {
				case e:CannotRedoException 	=>
					ERROR("redo failed", e)
			}
		}
	}
	
	// def getLimit	= undoManager.getLimit
	// def setLimit(limit:Int) { undoManager setLimit limit }
	
	private def install(text:JTextComponent) {
		text addPropertyChangeListener propertyListener
		
		val document	= text.getDocument
		document addUndoableEditListener undoableEditListener
		
		val actionMap	= text.getActionMap
		actionMap put (UNDO_ACTION,	undoAction)
		actionMap put (REDO_ACTION,	redoAction)
		
		val inputMap	= text.getInputMap
		inputMap put(UNDO_KEY_STROKE, UNDO_ACTION)
		inputMap put(REDO_KEY_STROKE, REDO_ACTION)
	}
	
	private def uninstall(text:JTextComponent) {
		text removePropertyChangeListener propertyListener
		
		val document	= text.getDocument
		document removeUndoableEditListener undoableEditListener
		
		val actionMap	= text.getActionMap
		actionMap remove UNDO_ACTION
		actionMap remove REDO_ACTION
		
		val inputMap	= text.getInputMap
		inputMap remove UNDO_KEY_STROKE
		inputMap remove REDO_KEY_STROKE
	}
	
	install(self)
}
