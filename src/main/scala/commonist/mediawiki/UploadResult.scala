package commonist.mediawiki

/** outcome of API#edit and API#upload */
sealed trait UploadResult
final case class UploadSuccess(fileName:String, pageTitle:String)	extends UploadResult
final case class UploadFailure(failureCode:String)					extends UploadResult
final case class UploadError(errorCode:String)						extends UploadResult
final case class UploadAborted(warnings:Set[UploadWarning])			extends UploadResult
