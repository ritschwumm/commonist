package commonist.util

import commonist.data._

import scala.xml._

import scutil.log._

/** 
 * Parse and extract image metadata from OAI-PMH 2.0 files.
 * See www.openarchives.org/OAI/openarchivesprotocol.html
 */
class OaiPmh2(doc:Elem,props:Map[String,String]) extends Logging {

  /** fix badly formatted XML files (escaped twice) **/
  private def text(e:NodeSeq) = {
    e.map(_.text).mkString("\n").trim()
     .replaceAll("&amp;", "&").replaceAll("&quot;", "\"").replaceAll("&apos;", "'").replaceAll("&lt;", "<").replaceAll("&gt;", ">")
  }

  private def artist(creator:String):String = {
    val prop = creator.replaceAll(" ", "_")
    if (props.contains(prop))
      return props(prop)
    return creator
  }

  private def permission(rights:String):String = {
    val lowercase = rights.toLowerCase()
    if (lowercase.contains("public domain")
     || lowercase.contains("domaine public")
     || lowercase.contains("gemeinfreiheit")
     || lowercase.contains("dominio público")
     )
      return "{{PD-old|PD-70}}"
    return rights
  }

  private def medium(format:String):String = {
    val lowercase = format.toLowerCase()
    if (lowercase.contains("photo") || lowercase.contains("foto"))
      return "{{Technique|photograph}}"
    return format
  }

  private def dimensions(format:String):String = {
    ".*; (\\d+) x (\\d+) cm ;.*".r.findAllMatchIn(format).foreach { m =>
      return "{{Size|unit=cm|height=" + m.group(1) + "|width=" + m.group(2) + "}}"
    }
    return ""
  }
  
  private def description(fullDescription:String,historyIndex:Int):String = {
    if (historyIndex >= 0)
      return fullDescription.substring(0, historyIndex).trim()
    return fullDescription
  }
  
  private def objectHistory(fullDescription:String,historyIndex:Int,length:Int):String = {
    if (historyIndex >= 0)
      return fullDescription.substring(historyIndex + length).trim()
    return ""
  }

  private def localized(text:String):String = {
    if (text != "") {
      return "{{" + props("lang") + "|" + text + "}}"
    }
    return text
  }

  /** Fills image metadata if found via its filename */
  def getImageData(data:ImageData):Option[ImageData] = {
    val filenameWithOutExt = data.file.getName().replaceFirst("[.][^.]+$", "");
    val list:List[Node] = (doc \\ "dc").find(dc => text(dc \ "relation").contains(filenameWithOutExt + ".")).toList
    if (list.size == 1) {
      val dc:Node = list(0)

      val historyMarker = props("historyMarker")
      val institution = props("institution")
      val prefix = props("prefix")
      val fonds = props("fonds")

      val fullDescription = text(dc \ "description")
      val historyIndex = fullDescription.indexOf(historyMarker)
      val format = text(dc \ "format").replaceAll("image/jpeg", "").trim()

      return Some(new ImageData(
          data.file,
          data.upload,
          // filename - title.ext
          (data.name.replaceFirst("[.][^.]+$", "") +
            " - " + text(dc \ "title") +
            data.name.substring(data.name.lastIndexOf(".")).replaceAll("\\.\\.", ".")),
          // description
          "{{Artwork\n" +
          "|ID={{" + institution + " - FET link|" + filenameWithOutExt.replaceAll(prefix, "") + "}}\n" +
          "|artist=" + artist(text(dc \ "creator")) + "\n" +
          "|credit line=\n" +
          "|date=" + text(dc \ "date") + "\n" +
          "|location=\n" +
          "|description=" + localized(description(fullDescription, historyIndex)) + "\n" +
          "|dimensions=" + dimensions(format) + "\n" +
          "|gallery={{Institution:" + institution + "}}\n" +
          "|medium=" + medium(format) + "\n" +
          "|object history=" + localized(objectHistory(fullDescription, historyIndex, historyMarker.length)) + "\n" +
          "|permission=" + permission(text(dc \ "rights")) + "\n" +
          "|references=\n" +
          "|source={{" + fonds + " - " + institution + "}}\n" +
          "|title=" + localized(text(dc \ "title")) + "\n" +
          "}}",
          text(dc \ "date"),
          data.coordinates,
          data.heading,
          data.categories
          ));
    } else if (list.size > 1) {
      WARN("Found several records for " + filenameWithOutExt)
    }
    return Option.empty
  }
}
