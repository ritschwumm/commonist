package commonist

import javax.swing.BorderFactory

/** constants used throughout the application */
object Constants {
	val VERSION						= BuildInfo.version
	val ENABLE_API_WRITE			= true	// if false the API is dry-running
	val ENABLE_GALLERY				= true
	
	val THUMBNAIL_DEFAULT_SIZE		= 192
	val THUMBNAIL_CACHE_SIZE		= 2000	// images
	val THUMBNAIL_SCALE_HEADROOM	= 250	// percent
	
//	val  FULLSIZE_MIN_FRAME_SIZE		= 32	// pixel
	val FULLSIZE_MAX_UNIT_INCREMENT	= 64	// pixel
	
	val IMAGELIST_UPDATE_DELAY		= 1500	// millis
	val IMAGELIST_UPDATE_COUNT		= 3		// count
	
	val INPUT_FIELD_WIDTH			= 24	// columns
	val INPUT_FIELD_HEIGHT			= 5 	// rows
	
	val PANEL_BORDER				= BorderFactory createEmptyBorder (2,2,2,2)
}
