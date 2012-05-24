package com.voltvoodoo.saplo4j.model
import java.io.Serializable
public open class Language(code : String?) : Serializable {
protected var code : String? = null
public open fun equals(other : Language?) : Boolean {
return other?.toString()?.equals(this.toString())!!
}
{
this.code = code
}
class object {
public var ENGLISH : Language? = Language("en")
public var SWEDISH : Language? = Language("sv")
private val serialVersionUID : Long = -2442762969929206780
}
}