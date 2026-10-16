package commonist.ui

import java.io.File
import java.util.{ Enumeration => JUEnumeration }
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

import scutil.core.implicits._
import scutil.jdk.implicits._

/** a TreeNode for a File in the DirectoryTree */
final class FileNode(val file:File) extends DefaultMutableTreeNode {
	private var allowsChildrenValue	= false

	def childNodes:Seq[FileNode]	=
		children()
		.asInstanceOf[JUEnumeration[FileNode]]
		.toIterator
		.toVector

	// NOTE without asInstanceOf scala chooses the Object constructor over the Object[] constructor
	def treePathClone:TreePath	=
		new TreePath(getPath.asInstanceOf[Array[Object]])

	/** ensures the node has a single child every directory below it */
	def update():Unit	= {
		removeAllChildren()

		val listed	= file childrenWhere { (file:File) => file.isDirectory && !file.isHidden }
		allowsChildrenValue	= listed.isDefined
		listed foreach { files =>
			files sortBy { _.getPath } map { new FileNode(_) } foreach add
		}
	}

	//------------------------------------------------------------------------------

	override def getAllowsChildren():Boolean	= allowsChildrenValue
	override def isLeaf():Boolean				= false
	override def toString():String				= file.getName.optionNonEmpty getOrElse file.getPath
}
