package commonist.util

import commonist.data._

import scala.xml._

import scutil.log._

/** 
 * Parse and extract image metadata from OAI-PMH 2.0 files.
 * See www.openarchives.org/OAI/openarchivesprotocol.html
 */
class OaiPmh2(doc:Elem, props:Map[String,String]) extends Logging {

	/** Extract text from node and fix badly formatted XML strings (escaped twice) **/
	private def text(e:NodeSeq) = {
		e.map(_.text).mkString("\n").trim()
		 .replaceAll("&amp;", "&").replaceAll("&quot;", "\"").replaceAll("&apos;", "'").replaceAll("&lt;", "<").replaceAll("&gt;", ">")
	}

	/**
	 * Formats name as follows: "filename_without_ext - title.ext"
	 * Makes sure we don't have two consecutive dots if title ends by a dot
	 */
	private def formatName(name:String, title:String):String = {
		name.replaceFirst("[.][^.]+$", "") + " - " + title + name.substring(name.lastIndexOf(".")).replaceAll("\\.\\.", ".")
	}

	/** Attempt to get a nicer artist value from properties, otherwise return raw creator */
	private def artist(creator:String):String = {
		val prop = creator.replaceAll(" ", "_")
		props get prop getOrElse creator
	}

	/** Detect public domain mention in various languages, otherwise return raw rights */
	private def permission(rights:String):String = {
		val lowercase = rights.toLowerCase()
		if (lowercase.contains("public domain")
		 || lowercase.contains("domaine public")
		 || lowercase.contains("gemeinfreiheit")
		 || lowercase.contains("dominio público")
		 )
			"{{PD-old|PD-70}}"
		else rights
	}

	/** Detect photographs from format, otherwise return raw format */
	private def medium(format:String):String = {
		val lowercase = format.toLowerCase()
		if (lowercase.contains("photo") || lowercase.contains("foto"))
			"{{Technique|photograph}}"
		else format
	}

	/** Detect size in centimeters, otherwise return raw format */
	private def dimensions(format:String):String = {
		".*; (\\d+) x (\\d+) cm ;.*".r.findAllMatchIn(format).foreach { m =>
			return "{{Size|unit=cm|height=" + m.group(1) + "|width=" + m.group(2) + "}}"
		}
		""
	}

	/** localize given string according to language defined in properties */
	private def localized(text:String):String = {
		if (text.nonEmpty)
			"{{" + props("lang") + "|" + text + "}}"
		else text
	}

	/** Artwork description */
	private def artwork(dc:Node, filenameWithOutExt:String):String = {
			val historyMarker = props("historyMarker")
			val institution = props("institution")
			val prefix = props("prefix")
			val fonds = props("fonds")

			val id = filenameWithOutExt.replaceAll(prefix, "")
			val fullDescription = text(dc \ "description")
			val historyIndex = fullDescription.indexOf(historyMarker)
			val objectHistory = if (historyIndex >= 0) fullDescription.substring(historyIndex + historyMarker.length).trim() else ""
			val description = if (historyIndex >= 0) fullDescription.substring(0, historyIndex).trim() else fullDescription
			val format = text(dc \ "format").replaceAll("image/jpeg", "").trim()

			"{{Artwork\n" +
			"|ID={{" + institution + " - FET link|" + id + "}}\n" +
			"|artist=" + artist(text(dc \ "creator")) + "\n" +
			"|credit line=\n" +
			"|date=" + text(dc \ "date") + "\n" +
			"|location=\n" +
			"|description=" + localized(description) + "\n" +
			"|dimensions=" + dimensions(format) + "\n" +
			"|gallery={{Institution:" + institution + "}}\n" +
			"|medium=" + medium(format) + "\n" +
			"|object history=" + localized(objectHistory) + "\n" +
			"|permission=" + permission(text(dc \ "rights")) + "\n" +
			"|references=\n" +
			"|source={{" + fonds + " - " + institution + "}}\n" +
			"|title=" + localized(text(dc \ "title")) + "\n" +
			"}}",
	}

	/** Fills image metadata if found via its filename */
	def getImageData(data:ImageData):Option[ImageData] = {
		val filenameWithOutExt = data.file.getName().replaceFirst("[.][^.]+$", "");
		val list:List[Node] = (doc \\ "dc").find(dc => text(dc \ "relation").contains(filenameWithOutExt + ".")).toList
		if (list.size == 1) {
			val dc:Node = list(0)

			Some(ImageData(
					data.file,
					data.upload,
					// "filename - title.ext"
					formatName(data.name, text(dc \ "title")),
					// {{Artwork}} description
					artwork(dc, filenameWithOutExt),
					text(dc \ "date"),
					data.coordinates,
					data.heading,
					data.categories
					))
		} else if (list.size > 1) {
			WARN("Found several records for " + filenameWithOutExt)
			Option.empty
		} else {
			Option.empty
		}
	}
}
