package commonist.data

import scutil.lang.ISeq

/** Data edited in an ImageListUI */
final case class ImageListData(imageDatas:ISeq[ImageData]) {
	def hasSelected:Boolean			= imageDatas exists	{ _.upload }
	def selected:ISeq[ImageData]	= imageDatas filter	{ _.upload }
}
