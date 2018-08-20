package commonist.mediawiki

trait UploadCallback {
	def progress(bytes:Long)
	
	/** whether to ignore the given warnings, if false the upload is aborted */
	def ignore(warnings:Set[UploadWarning]):Boolean
}
