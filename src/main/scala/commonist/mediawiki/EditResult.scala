package commonist.mediawiki

/** outcome of API#edit and API#newsection */
sealed trait EditResult
final	case class EditSuccess(pageTitle:String)		extends EditResult
final	case class EditFailure(failureCode:String)		extends EditResult
final	case class EditError(errorCode:String)			extends EditResult
		case object EditAborted							extends EditResult
