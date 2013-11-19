package com.voltvoodoo.saplo4j.model
import java.io.Serializable
public open class Language(code : String) : Serializable {
protected var code : String = 0
public override fun toString() : String {
return this.code
}
{
this.code = code
}
}
open class Base() {
open fun test() {
}
open fun toString() : String {
return "BASE"
}
}
open class Child() : Base() {
override fun test() {
}
override fun toString() : String {
return "Child"
}
}