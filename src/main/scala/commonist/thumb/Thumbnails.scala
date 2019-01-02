package commonist.thumb

import java.lang.{ Integer => JInteger }
import java.io._
import java.awt.{ List => _, _ }
import java.awt.geom._
import java.awt.image._
import javax.swing._
import javax.imageio._

import scutil.base.implicits._
import scutil.io.implicits._
import scutil.log._
import scutil.gui.implicits._

import commonist.Constants
import commonist.util.Settings

/** manages thumbnail images */
final class Thumbnails(cache:FileCache) extends Logging {
	var	maxSize	= Constants.THUMBNAIL_DEFAULT_SIZE

	def loadSettings(settings:Settings) {
		maxSize	= settings getIntOrElse ("thumbnails.maxSize", maxSize)
	}
	
	def saveSettings(settings:Settings) {
		settings setInt ("thumbnails.maxSize", maxSize)
	}
	
	def getMaxSize:Int	= maxSize
	
	/** creates a thumbnail Icon from an image File or returns null */
	def thumbnail(file:File):Option[Icon] =
			try {
				cachedThumbnail(file) map { new ImageIcon(_) }
			}
			catch { case e:Exception =>
				INFO("cannot create thumbnail", file, e)
				None
			}
	
	/** make a thumbnail or return a cached version */
	private def cachedThumbnail(file:File):Option[Image] =
			// BETTER give the cache a loader instead of talking to it here
			// try to get cached thumb
			(cache get file) map { ImageIO read _ } orElse
			// read original and make thumb
			readSubsampled(file) map makeThumbnail doto {
				_ foreach { thumb =>
					// cache thumb
					val thumbFile2	= cache put file
					val	success		= ImageIO write (thumb, "jpg", thumbFile2)
					if (!success) {
						WARN("could not create thumbnail", thumbFile2)
						cache remove file
					}
				}
			}
	
	/** makes a thumbnail from an image */
	private def makeThumbnail(image:BufferedImage):BufferedImage = {
		val scale	= maxSize.toDouble / (image.getWidth max image.getHeight)
		if (scale >= 1.0)	return image
		
		// TODO check more image types
		// seen: TYPE_BYTE_GRAY and TYPE_BYTE_INDEXED,
		// TYPE_CUSTOM			needs conversion or throws an ImagingOpException at transformation time
		// TYPE_3BYTE_BGR		works without conversion
		// TYPE_BYTE_GRAY		needs conversion or creates distorted grey version
		// TYPE_INT_RGB			needs conversion or creates distorted grey version
		// TYPE_BYTE_INDEXED	works, but inverts color if converted to TYPE_3BYTE_BGR
		
		// normalize image type
		val normalizeTypes	= Set(BufferedImage.TYPE_3BYTE_BGR, BufferedImage.TYPE_BYTE_INDEXED, BufferedImage.TYPE_CUSTOM)
		val image2 =
				if (normalizeTypes contains image.getType) {
					val normalized	= new BufferedImage(
							image.getWidth,
							image.getHeight,
							BufferedImage.TYPE_3BYTE_BGR)
					normalized doto {
						_.getGraphics use { g =>
							g drawImage (image, 0, 0, null)
						}
					}
				}
				else {
					image
				}
		
		val size	= new Dimension(
				(image2.getWidth * scale).toInt,
				(image2.getHeight * scale).toInt)
		val	thumb	= new BufferedImage(
				size.width,
				size.height,
				image2.getType)
		val op		= new AffineTransformOp(
				new AffineTransform(scale, 0, 0, scale, 0, 0),	// AffineTransform.getScaleInstance(sx, sy)
				AffineTransformOp.TYPE_BILINEAR)				// TYPE_NEAREST_NEIGHBOR, TYPE_BILINEAR, TYPE_BICUBIC
		
		op filter (image2, thumb)
		thumb
	}
	
	/** scales down when the image is too big */
	private def readSubsampled(input:File):Option[BufferedImage] = {
		val stream = ImageIO createImageInputStream input
		if (stream == null)	{
			ERROR(show"cannot create ImageInputStream for file", input)
			return None
		}
		stream use { stream =>
			val it	= ImageIO getImageReaders stream
			if (!it.hasNext) {
				ERROR("cannot get ImageReader for file", input)
				return None
			}
			
			it.next use { reader =>
				reader setInput (stream, true, true)
				
				val param		= reader.getDefaultReadParam
				
				val imageIndex	= 0
		
				val sizeX		= reader getWidth	imageIndex
				val sizeY		= reader getHeight	imageIndex
				val size		= sizeX min sizeY
				val scale		= size / maxSize
				val sampling	= JInteger highestOneBit (scale * 100 / Constants.THUMBNAIL_SCALE_HEADROOM)
				
				// BETTER could scale at load time!
				if (sampling > 1)	param setSourceSubsampling (sampling, sampling, 0, 0)
				val image	= reader read	(imageIndex, param)
				
				Some(image)
			}
		}
	}
}
