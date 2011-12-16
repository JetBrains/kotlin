namespace com.voltvoodoo.saplo4j.model
import java.io.Serializable
public open class Language(code : String?) : Serializable {
{
this.code = code
}
protected var code : String? = null
open public fun equals(other : Language?) : Boolean {
return other?.toString()?.equals(this.toString()).sure()
}
class object {
public var ENGLISH : Language? = Language("en")
public var SWEDISH : Language? = Language("sv")
private val serialVersionUID : Long = (-2442762969929206780)
}
}