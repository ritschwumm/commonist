package commonist.mediawiki

object LoginResult {
	final case class Success(userName:String)		extends LoginResult
	final case class Failure(failureCode:String)	extends LoginResult
	final case class Error(errorCode:String)		extends LoginResult
}

/** outcome of API#login */
sealed trait LoginResult
