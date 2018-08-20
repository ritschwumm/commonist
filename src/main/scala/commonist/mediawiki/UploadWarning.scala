package commonist.mediawiki

import scutil.lang.ISeq

sealed trait UploadWarning
final case class UploadWarningWasDeleted(name:String)			extends UploadWarning
final case class UploadWarningExists(name:String)				extends UploadWarning
// BETTER use a Nes
final case class UploadWarningDuplicate(names:ISeq[String])		extends UploadWarning
final case class UploadWarningDuplicateArchive(name:String)		extends UploadWarning
