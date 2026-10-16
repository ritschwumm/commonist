package commonist.mediawiki

object UploadWarning {
	final case class WasDeleted(name:String)			extends UploadWarning
	final case class Exists(name:String)				extends UploadWarning
	// BETTER use a Nes
	final case class Duplicate(names:Seq[String])		extends UploadWarning
	final case class DuplicateArchive(name:String)		extends UploadWarning
}

sealed trait UploadWarning
