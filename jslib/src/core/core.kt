package js;

import js.annotations.LibraryFun
import js.annotations.LibraryClass

LibraryFun("println")
fun println()
LibraryFun("println")
fun println(s : Any?)
LibraryFun("print")
fun print(s : Any?)
LibraryFun("parseInt")
fun parseInt(s : String) : Int
LibraryClass
open class Exception()
LibraryClass
class NumberFormatException() : Exception()
