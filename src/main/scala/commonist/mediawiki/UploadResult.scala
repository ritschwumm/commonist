package commonist.mediawiki

object UploadResult {
	final case class Success(fileName:String, pageTitle:String)	extends UploadResult
	final case class Failure(failureCode:String)				extends UploadResult
	final case class Error(errorCode:String)					extends UploadResult
	final case class Aborted(warnings:Set[UploadWarning])		extends UploadResult
}

/** outcome of API#edit and API#upload */
sealed trait UploadResult
