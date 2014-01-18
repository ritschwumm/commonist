package commonist.util

import java.io._
import java.text._
import java.util.Date
import java.math.MathContext
import java.math.RoundingMode

import org.apache.sanselan.ImageReadException
import org.apache.sanselan.Sanselan
import org.apache.sanselan.common.IImageMetadata
import org.apache.sanselan.common.RationalNumber
import org.apache.sanselan.formats.jpeg.JpegImageMetadata
import org.apache.sanselan.formats.tiff.TiffField
import org.apache.sanselan.formats.tiff.TiffImageMetadata
import org.apache.sanselan.formats.tiff.constants.TagInfo
import org.apache.sanselan.formats.tiff.constants.ExifTagConstants._
import org.apache.sanselan.formats.tiff.constants.TiffTagConstants._
import org.apache.sanselan.formats.tiff.constants.TiffDirectoryConstants._
import org.apache.sanselan.formats.tiff.constants.GPSTagConstants._

import scutil.implicits._
import scutil.math.BigRational
import scutil.log._

object EXIF extends Logging {
	val NONE	= EXIF(None, None, None, None)
	
	def extract(file:File):EXIF = 
			try {
				(Sanselan getMetadata file) match {
					case meta:JpegImageMetadata	=>
						INFO("found EXIF data", file)
						EXIF(
								getDocumentName(meta), 
								getImageDescription(meta), 
								getDate(meta),
								getGPS(meta))
					case _ =>
						INFO("no EXIF data found", file)
						NONE
				}
			}
			catch { case e:Exception	=>	// ImageReadException, IOException
				DEBUG("cannot read file", file, e.getMessage)
				NONE
			}
	
	//------------------------------------------------------------------------------
	
	/*
	// NOTE this doesn't work in sanselan 0.97 because FieldTypeRational#getSimpleValue
	// returns either a RationalNumber or an Array of RationalNumber whereas
	// EXIF#getGPS expects to get an Array of exactly 3 RationalNumber objects
	private def getGPS(metaData:JpegImageMetadata):Option[GPS] =
			for {
				exif	<- metaData.getExif.nullOption
				gps		<- exif.getGPS.nullOption
			}
			yield GPS(gps.getLatitudeAsDegreesNorth, gps.getLongitudeAsDegreesEast)
	*/
	
	private def getGPS(metaData:JpegImageMetadata):Option[GPS] =
			for {
				exif			<- metaData.getExif.guardNotNull
				gpsDir			<- (exif findDirectory DIRECTORY_TYPE_GPS).guardNotNull
				
				latitudeRef		<- (gpsDir findField GPS_TAG_GPS_LATITUDE_REF).guardNotNull
				latitudeVal		<- (gpsDir findField GPS_TAG_GPS_LATITUDE).guardNotNull
				latitude		<- part(latitudeVal, latitudeRef, scala.collection.immutable.Map("n" -> 1, "s" -> -1))
				
				longitudeRef	<- (gpsDir findField GPS_TAG_GPS_LONGITUDE_REF).guardNotNull
				longitudeVal	<- (gpsDir findField GPS_TAG_GPS_LONGITUDE).guardNotNull
				longitude		<- part(longitudeVal, longitudeRef, scala.collection.immutable.Map("e" -> 1, "w" -> -1))
			}
			yield {
				GPS(latitude, longitude)
			}
	
	private def part(valueField:TiffField, signField:TiffField, signCalc:PartialFunction[String,Int]):Option[BigDecimal] =
			for {
				sign	<- signCalc lift signField.getStringValue.trim.toLowerCase
				value	<- decimal(valueField.getValue)
			}
			yield {
				value * sign
			}
	
	// exif	 		34.00, 57.00, 57.03, 1.47
	// galculator	34.9658498611
	
	// sum			986036911/28200000
	// galculator	34.9658479078
	// mein code	34.9658479
	// 14257/250	5703/100
	
			
	private def decimal(value:AnyRef):Option[BigDecimal] = value match {
		// case dms:Array[RationalNumber] if dms.length == 3	=>
		// 	val	all	= dms map bigRational
		// 	val sum	= all(0) / BigRational(1) + all(1) / BigRational(60) + all(2) / BigRational(3600)
		// 	Some(bigDecimal(sum))
		case dms:Array[RationalNumber] if dms.length > 0 =>
			val	factors	= Stream.iterate(1)(60 *) map { BigRational(_) }
			val sum		= dms.toList map bigRational zip factors map { case (v,f) => v / f } reduceLeft (_+_)
			Some(bigDecimal(sum))
		case d:RationalNumber =>
			val	sum	= bigRational(d)
			Some(bigDecimal(sum))
		case x =>
			DEBUG("unexpected value", x)
			None			
	}
	private def bigRational(value:RationalNumber):BigRational	= BigRational(value.numerator, value.divisor)
	private def bigDecimal(value:BigRational):BigDecimal		= new BigDecimal(value toBigDecimal gpsPrecision)
	private val gpsPrecision:MathContext						= new MathContext(12, RoundingMode.HALF_EVEN)
		 
	//------------------------------------------------------------------------------
	
	private def getImageDescription(metaData:JpegImageMetadata):Option[String] =
			getString(metaData, EXIF_TAG_IMAGE_DESCRIPTION)
	
	private def getDocumentName(metaData:JpegImageMetadata):Option[String] =
			getString(metaData, EXIF_TAG_DOCUMENT_NAME)
	
	private def getDate(metaData:JpegImageMetadata):Option[Date] =
			getDate(metaData, EXIF_TAG_DATE_TIME_ORIGINAL)	orElse	// DateTimeOriginal
			getDate(metaData, EXIF_TAG_CREATE_DATE)			orElse	// DateTimeDigitized
			getDate(metaData, TIFF_TAG_DATE_TIME)					// DateTime
			
	private def getString(metaData:JpegImageMetadata, tagInfo:TagInfo):Option[String] =
			getValueDescription(metaData, tagInfo) map { _ replaceAll ("^'|'$", "") } 
			
	private def getDate(metaData:JpegImageMetadata, tagInfo:TagInfo):Option[Date] =
			getValueDescription(metaData, tagInfo) flatMap parseDate _ 
			
	// @see http://www.awaresystems.be/imaging/tiff/tifftags/privateifd/exif/datetimeoriginal.html
	private def parseDate(s:String):Option[Date] = 
			try { Some(new SimpleDateFormat("''yyyy:MM:dd HH:mm:ss''") parse s) }
			catch { case e:ParseException => DEBUG("cannot parse date", s); None }
			
	private def getValueDescription(metaData:JpegImageMetadata, tagInfo:TagInfo):Option[String] =
			Option(metaData findEXIFValue tagInfo) map { _.getValueDescription }
}

// TODO use name and description to fill the GUI
case class EXIF(name:Option[String], description:Option[String], date:Option[Date], gps:Option[GPS])
case class GPS(latitude:BigDecimal, longitude:BigDecimal)
