package commonist.ui

import java.io.File
import java.util.{ Enumeration => JUEnumeration }
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

import scutil.lang.ISeq
import scutil.implicits._

/** a TreeNode for a File in the DirectoryTree */
final class FileNode(val file:File) extends DefaultMutableTreeNode {
	private var allowsChildrenValue	= false
	
	def childNodes:ISeq[FileNode]	= {
		// TODO scutil 0.58.0
		val enum	= children().asInstanceOf[JUEnumeration[FileNode]]
		val iter	= new Iterator[FileNode] {
			def hasNext	= enum.hasMoreElements
			def next	= enum.nextElement
		}
		iter.toVector
	}
	
	// NOTE without asInstanceOf scala chooses the Object constructor over the Object[] constructor
	def treePathClone:TreePath	= new TreePath(getPath.asInstanceOf[Array[Object]])
	
	/** ensures the node has a single child every directory below it */
	def update() {
		removeAllChildren()
		
		val listed	= file childrenWhere { file:File => file.isDirectory && !file.isHidden }
		allowsChildrenValue	= listed.isDefined
		listed foreach { files =>
			files sortBy { _.getPath } map { new FileNode(_) } foreach add
		}
	}
	
	//------------------------------------------------------------------------------
	
	override def getAllowsChildren():Boolean	= allowsChildrenValue
	override def isLeaf():Boolean				= false
	override def toString():String				= file.getName.guardNonEmpty getOrElse file.getPath
}
