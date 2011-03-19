package commonist.data

/** Data edited in an ImageListUI */
case class ImageListData(imageDatas:List[ImageData]) {
	def hasSelected:Boolean			= imageDatas find	{ _.upload } isDefined;
	def selected:List[ImageData]	= imageDatas filter	{ _.upload }
}
