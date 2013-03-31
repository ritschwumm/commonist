package commonist.data

/** Data edited in an ImageListUI */
case class ImageListData(imageDatas:Seq[ImageData]) {
	def hasSelected:Boolean		= imageDatas exists	{ _.upload }
	def selected:Seq[ImageData]	= imageDatas filter	{ _.upload }
}
