package js;

import js.library
import js.native
import java.util.*;
import java.lang.*;


native
val noImpl : Nothing = throw Exception();

library("println")
fun println() {}
library("println")
fun println(s : Any?) {}
library("print")
fun print(s : Any?) {}
//TODO: consistent parseInt
library("parseInt")
fun parseInt(s : String) : Int = js.noImpl
library
open class Exception() : Throwable() {}
library
class NumberFormatException() : Exception() {}

native
fun setTimeout(callback : ()-> Unit) {}

native
fun setInterval(callback : ()-> Unit, ms : Int) {}
native
fun setInterval(callback : ()-> Unit) {}


native
open class DomElement() {
    val offsetLeft = 0.0;
    val offsetTop = 0.0;
    val offsetParent : DomElement? = DomElement();
}
