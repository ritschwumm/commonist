package commonist.data

import java.io.File

/** Data edited in an ImageUI */
final case class ImageData(
	file:File,
	upload:Boolean,
	name:String,
	description:String,
	date:String,
	coordinates:String,
	heading:String,
	categories:String
)
