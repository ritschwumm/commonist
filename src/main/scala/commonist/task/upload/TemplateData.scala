package commonist.task.upload

import commonist.data._

import scala.reflect.BeanProperty

case class Common(
	@BeanProperty description:String,
	@BeanProperty date:String,
	@BeanProperty source:String,
	@BeanProperty author:String,
	@BeanProperty permission:String,
	@BeanProperty licenseTemplate:String,
	@BeanProperty licenseDescription:String,
	@BeanProperty categories:String
)

case class Batch(
	@BeanProperty uploads:java.util.List[Upload],
	@BeanProperty successes:java.util.List[Upload],
	@BeanProperty failures:java.util.List[Upload]
) 

case class Upload(
	@BeanProperty name:String,			// without the File namespace
	@BeanProperty title:String,			// to be used in a link
	@BeanProperty error:String,			// null if upload went ok
	@BeanProperty previous:String,		// null for the first
	@BeanProperty next:String,			// null for the last
	
	@BeanProperty description:String,
	@BeanProperty date:String,
	@BeanProperty categories:String,
	
	@BeanProperty coordinates:String,	// raw string
	@BeanProperty latitude:String,		// null if raw string could not be parsed
	@BeanProperty longitude:String		// null if raw string could not be parsed
)
