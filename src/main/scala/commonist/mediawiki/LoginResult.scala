package commonist.mediawiki

/** outcome of API#login */
sealed trait LoginResult
final case class LoginSuccess(userName:String)		extends LoginResult
final case class LoginFailure(failureCode:String)	extends LoginResult
final case class LoginError(errorCode:String)		extends LoginResult
