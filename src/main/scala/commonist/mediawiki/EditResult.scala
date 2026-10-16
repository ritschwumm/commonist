package commonist.mediawiki

object EditResult {
	final case class	Success(pageTitle:String)		extends EditResult
	final case class	Failure(failureCode:String)		extends EditResult
	final case class	Error(errorCode:String)			extends EditResult
	case object			Aborted							extends EditResult
}

/** outcome of API#edit and API#newsection */
sealed trait EditResult
