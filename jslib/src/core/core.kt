package js;

import js.annotations.LibraryFun
import js.annotations.LibraryClass
import js.annotations.Native

LibraryFun("println")
fun println() {}
LibraryFun("println")
fun println(s : Any?) {}
LibraryFun("print")
fun print(s : Any?) {}
LibraryFun("parseInt")
fun parseInt(s : String) : Int = 0
LibraryClass
open class Exception() {}
LibraryClass
class NumberFormatException() : Exception() {}

Native
fun setTimeout(callback : ()-> Unit) {}

Native
fun setInterval(callback : ()-> Unit, ms : Int) {}
Native
fun setInterval(callback : ()-> Unit) {}