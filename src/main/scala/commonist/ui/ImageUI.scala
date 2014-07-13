package commonist.ui

import java.io.File
import java.awt.{ List => AwtList, _ }
import java.awt.event._
import javax.swing._

import scutil.lang._
import scutil.implicits._
import scutil.color._
import scutil.gui.implicits._
import scutil.gui.GridBagDSL._
import scutil.gui.CasterInstances._

import scmw._

import commonist.Constants
import commonist.data._
import commonist.util._

trait ImageUICallback {
	def updateSelectStatus()
}
	
/** a data editor with a thumbnail preview for an image File */
final class ImageUI(file:File, icon:Option[Icon], thumbnailMaxSize:Int, programHeading:String, programIcon:Image, callback:ImageUICallback) extends JPanel {
	private val thumbDimension = new Dimension(thumbnailMaxSize, thumbnailMaxSize)
	
	private var uploadSuccessful:Option[Boolean]	= None
	
	//------------------------------------------------------------------------------
	
	private val stateView	= new JLabel(null, null, SwingConstants.CENTER)
	stateView setHorizontalTextPosition SwingConstants.CENTER
	updateStateView()
	
	private val imageView	= new JLabel(null, null, SwingConstants.CENTER)
	imageView setBackground rgb"eeeeee".toColor
	imageView setOpaque true
	/*
	BETTER add tooltip
	imageView setToolTipText (
		file.Name + " (" + TextUtil.human(file.length()) + " bytes)"
	)
	)
	*/
	imageView setHorizontalTextPosition	SwingConstants.CENTER
	imageView setVerticalTextPosition	SwingConstants.CENTER
	imageView setPreferredSize	thumbDimension
	imageView setMinimumSize	thumbDimension
	imageView setMaximumSize	thumbDimension

	private val uploadLabel			= new JLabel(Messages text "image.upload")
	private val nameLabel			= new JLabel(Messages text "image.name")
	private val descriptionLabel	= new JLabel(Messages text "image.description")
	private val dateLabel			= new JLabel(Messages text "image.date")
	private val coordinatesLabel	= new JLabel(Messages text "image.coordinates")
	private val categoriesLabel		= new JLabel(Messages text "image.categories")
	
	private val uploadEditor		= new JCheckBox(null.asInstanceOf[Icon], false)
	private val nameEditor			= new JTextField(Constants.INPUT_FIELD_WIDTH)	with TextComponentUndo
	private val descriptionEditor	= new JTextArea(Constants.INPUT_FIELD_HEIGHT, Constants.INPUT_FIELD_WIDTH)		with TextComponentUndo
	private val dateEditor			= new JTextField(Constants.INPUT_FIELD_WIDTH)	with TextComponentUndo
	private val coordinatesEditor	= new JTextField(Constants.INPUT_FIELD_WIDTH)	with TextComponentUndo
	private val categoriesEditor	= new JTextField(Constants.INPUT_FIELD_WIDTH)	with TextComponentUndo
	
	UIUtil2 tabMovesFocus descriptionEditor
	descriptionEditor setLineWrap		true
	descriptionEditor setWrapStyleWord	true
	coordinatesEditor setToolTipText 	(Messages text "image.coordinates.tooltip")
	categoriesEditor  setToolTipText	(Messages text "image.categories.tooltip")
	
	UIUtil2 scrollVisibleOnFocus (uploadEditor,			this)
	UIUtil2 scrollVisibleOnFocus (nameEditor,			this)
	UIUtil2 scrollVisibleOnFocus (descriptionEditor,	this)
	UIUtil2 scrollVisibleOnFocus (dateEditor,			this)
	UIUtil2 scrollVisibleOnFocus (coordinatesEditor,	this)
	UIUtil2 scrollVisibleOnFocus (categoriesEditor,		this)
	
	private val descriptionScroll	=
			new JScrollPane(
				descriptionEditor, 
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, 
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
			)

//		setBorder(
//			BorderFactory.createCompoundBorder(
//				//BorderFactory.createCompoundBorder(
//					BorderFactory.createRaisedBevelBorder(),
//				//	BorderFactory.createLoweredBevelBorder()
//				//),
//				BorderFactory.createEmptyBorder(2,0,2,0)
//			)
//		)
	setBorder(BorderFactory.createEmptyBorder(5,5,5,5))

	//------------------------------------------------------------------------------
	//## layout
	
	setLayout(new GridBagLayout)
	
	// labels and editors
	
	add(uploadLabel,		GBC pos (0,0) size (1,1) weight (0,0) anchor EAST		fill NONE		insetsTLBR (0,4,0,4))
	add(uploadEditor,		GBC pos (1,0) size (1,1) weight (1,0) anchor WEST		fill HORIZONTAL insetsTLBR (0,0,0,0))

	add(nameLabel,			GBC pos (0,1) size (1,1) weight (0,0) anchor EAST		fill NONE		insetsTLBR (0,4,0,4))
	add(nameEditor,			GBC pos (1,1) size (1,1) weight (1,0) anchor WEST		fill HORIZONTAL	insetsTLBR (0,0,0,0))
	
	add(descriptionLabel, 	GBC pos (0,2) size (1,1) weight (0,0) anchor NORTHEAST	fill NONE 		insetsTLBR (0,4,0,4))
	add(descriptionScroll,	GBC pos (1,2) size (1,1) weight (1,1) anchor WEST		fill BOTH 		insetsTLBR (0,0,0,0))
	
	add(dateLabel, 			GBC pos (0,3) size (1,1) weight (0,0) anchor EAST		fill NONE 		insetsTLBR (0,4,0,4))
	add(dateEditor,			GBC pos (1,3) size (1,1) weight (1,0) anchor WEST		fill HORIZONTAL insetsTLBR (0,0,0,0))
	
	add(coordinatesLabel, 	GBC pos (0,4) size (1,1) weight (0,0) anchor EAST		fill NONE 		insetsTLBR (0,4,0,4))
	add(coordinatesEditor,	GBC pos (1,4) size (1,1) weight (1,0) anchor WEST		fill HORIZONTAL insetsTLBR (0,0,0,0))
	
	add(categoriesLabel,	GBC pos (0,5) size (1,1) weight (0,0) anchor EAST		fill NONE 		insetsTLBR (0,4,0,4))
	add(categoriesEditor,	GBC pos (1,5) size (1,1) weight (1,0) anchor WEST		fill HORIZONTAL insetsTLBR (0,0,0,0))
	
	// state and image
	
	add(stateView,			GBC pos (2,0) size (1,1) weight (0,0) anchor CENTER		fill NONE 		insetsTLBR (0,4,0,4))
	add(imageView,			GBC pos (2,1) size (1,5) weight (0,0) anchor SOUTHWEST	fill NONE 		insetsTLBR (0,4,0,4))
	
	//------------------------------------------------------------------------------
	//## wiring
	
	// update select status on upload checkbox changes
	uploadEditor onActionPerformed { _ => 
		callback.updateSelectStatus() 
	}
	// open full size view on click
	imageView onMouseClicked { ev =>
		// LMB only
		if (ev.getButton == MouseEvent.BUTTON1)	{
			//if (imageView.Icon != null)
			displayFullImage()
		}
	}
	
	//------------------------------------------------------------------------------
	//## init
	
	imageView setToolTipText (Messages message ("image.tooltip", file.getName, TextUtil2.human(file.length)))
	imageView setIcon		icon.orNull
	imageView setText		(icon cata (Messages text "image.nothumb", constant("")))
	
	// BETTER move unparsers and parsers together
	
	private val	exif		= EXIF extract file
	private val exifDate	= exif.date	map { _ format "yyyy-MM-dd HH:mm:ss" } getOrElse ""
	private val exifGPS		= exif.gps	map { it => it.latitude.toString + "," + it.longitude.toString } getOrElse ""
	private val exifDesc	= exif.description getOrElse ""
	private val fixedName	= Filename fix file.getName
	
	uploadEditor		setSelected	false
	nameEditor			setText		fixedName
	descriptionEditor	setText		exifDesc
	dateEditor			setText		exifDate
	coordinatesEditor	setText		exifGPS
	categoriesEditor	setText		""
	
	// BETTER could be a trait
	override def getMaximumSize():Dimension =
			new Dimension(
				super.getMaximumSize.width,
				super.getPreferredSize.height
			)

	/** returns true when this file should be uploaded */
	def isUploadSelected():Boolean = uploadEditor.isSelected
	
	/** sets whether this file should be uploaded */
	def setUploadSelected(selected:Boolean) { uploadEditor.setSelected(selected) }
	
	def getUploadSuccessful:Option[Boolean] = uploadSuccessful
	
	def setUploadSuccessful(uploadSuccessful:Option[Boolean]) {
		this.uploadSuccessful = uploadSuccessful
		updateStateView()
	}
	
	private def updateStateView() {
		val	label	=
				Messages text (
					uploadSuccessful match {
						case Some(true)		=> "image.status.success"
						case Some(false)	=> "image.status.failure"
						case None			=> "image.status.none"
					}
				)
		stateView setText label
	}
	
	/** gets all data edit in this UI */
	def getData:ImageData =
			ImageData(
				file,
				uploadEditor.isSelected,
				nameEditor.getText,
				descriptionEditor.getText,
				dateEditor.getText,
				coordinatesEditor.getText,
				categoriesEditor.getText
			)       
			
	private def displayFullImage() {
		FullImageWindow display (file, programHeading, programIcon)
	}
}
