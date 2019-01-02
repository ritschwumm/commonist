package commonist.util

import java.io._
import java.text._
import java.util.Date
import java.math.MathContext
import java.math.RoundingMode

import org.apache.sanselan.Sanselan
import org.apache.sanselan.common.RationalNumber
import org.apache.sanselan.formats.jpeg.JpegImageMetadata
import org.apache.sanselan.formats.tiff.TiffDirectory
import org.apache.sanselan.formats.tiff.TiffField
import org.apache.sanselan.formats.tiff.constants.TagInfo
import org.apache.sanselan.formats.tiff.constants.ExifTagConstants._
import org.apache.sanselan.formats.tiff.constants.TiffTagConstants._
import org.apache.sanselan.formats.tiff.constants.TiffDirectoryConstants._
import org.apache.sanselan.formats.tiff.constants.GPSTagConstants._

import scutil.base.implicits._
import scutil.io.implicits._
import scutil.number.BigRational
import scutil.log._

object EXIF extends Logging {
	val NONE	= EXIF(None, None, None, None, None)

	def extract(file:File):EXIF =
			try {
				(Sanselan getMetadata file) match {
					case meta:JpegImageMetadata	=>
						INFO("found EXIF data in", file)
						EXIF(
							getDocumentName(meta),
							getImageDescription(meta),
							getDate(meta),
							getGPS(meta),
							getHeading(meta)
						) doto { it =>
							DEBUG("data found", it.toString)
						}
					case _ =>
						INFO("no EXIF data found", file)
						NONE
				}
			}
			catch { case e:Exception	=>	// ImageReadException, IOException
				DEBUG("cannot read file", file, e)
				NONE
			}

	//------------------------------------------------------------------------------

	private def getHeading(metaData:JpegImageMetadata):Option[BigDecimal] =
			for {
				gpsDir			<- getGpsDirectory(metaData)

				directionVal	<- (gpsDir findField GPS_TAG_GPS_IMG_DIRECTION).optionNotNull
				direction		<- decimal(directionVal.getValue)
				//directionRef	<- (gpsDir findField GPS_TAG_GPS_IMG_DIRECTION_REF).optionNotNull
				//GPS_TAG_GPS_IMG_DIRECTION_REF_VALUE_MAGNETIC_NORTH
				//GPS_TAG_GPS_IMG_DIRECTION_REF_VALUE_TRUE_NORTH
			}
			yield direction

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
				gpsDir			<- getGpsDirectory(metaData)

				latitudeRef		<- (gpsDir findField GPS_TAG_GPS_LATITUDE_REF).optionNotNull
				latitudeVal		<- (gpsDir findField GPS_TAG_GPS_LATITUDE).optionNotNull
				latitude		<- part(latitudeVal, latitudeRef, Map(
										GPS_TAG_GPS_LATITUDE_REF_VALUE_NORTH -> +1,
										GPS_TAG_GPS_LATITUDE_REF_VALUE_SOUTH -> -1))

				longitudeRef	<- (gpsDir findField GPS_TAG_GPS_LONGITUDE_REF).optionNotNull
				longitudeVal	<- (gpsDir findField GPS_TAG_GPS_LONGITUDE).optionNotNull
				longitude		<- part(longitudeVal, longitudeRef, Map(
										GPS_TAG_GPS_LONGITUDE_REF_VALUE_EAST -> +1,
										GPS_TAG_GPS_LONGITUDE_REF_VALUE_WEST -> -1))
			}
			yield GPS(latitude, longitude)

	private def getGpsDirectory(metaData:JpegImageMetadata):Option[TiffDirectory] =
			for {
				exif			<- metaData.getExif.optionNotNull
				gpsDir			<- (exif findDirectory DIRECTORY_TYPE_GPS).optionNotNull
			}
			yield gpsDir

	private def part(valueField:TiffField, signField:TiffField, signCalc:Map[String,Int]):Option[BigDecimal] =
			for {
				// TODO why is this case-insensitive?
				sign	<- signCalc collectFirst { case (k,v) if k equalsIgnoreCase signField.getStringValue.trim => v }
				value	<- decimal(valueField.getValue)
			}
			yield value * sign


	private def decimal(value:AnyRef):Option[BigDecimal] =
			value match {
				// case dms:Array[RationalNumber] if dms.length == 3	=>
				// 	val	all	= dms map bigRational
				// 	val sum	= all(0) / BigRational(1) + all(1) / BigRational(60) + all(2) / BigRational(3600)
				// 	Some(bigDecimal(sum))
				case dms:Array[RationalNumber] if dms.length > 0 =>
					val	factors	= Stream.iterate(1)(60 * _) map (_.toLong) map BigRational.fromLong
					val sum		= dms.toVector map bigRational zip factors map { case (v,f) => v / f } reduceLeft (_+_)
					Some(bigDecimal(sum))
				case d:RationalNumber =>
					val	sum	= bigRational(d)
					Some(bigDecimal(sum))
				case x =>
					DEBUG("unexpected value", x.toString)
					None
			}

	// NOTE we don't have a Show instance for RationalNumber
	private def bigRational(value:RationalNumber):BigRational	= BigRational fromLongs (value.numerator, value.divisor) getOrError show"division by zero converting ${value.toString}"
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

// BETTER use name and description to fill the GUI
final case class EXIF(name:Option[String], description:Option[String], date:Option[Date], gps:Option[GPS], heading:Option[BigDecimal])
final case class GPS(latitude:BigDecimal, longitude:BigDecimal)
