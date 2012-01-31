package raphael

import js.annotations.*;
import js.*;

NativeClass
class Element() {

    fun attr(name : String, value : String) : Element = this
    fun attr(nameToValue : Json) : Element = this

    fun mouseover(handler : () -> Unit) = this

    fun getTotalLength() : Double = 0.0
    //fun mouse
}

NativeClass
class Paper() {
    fun path(pathString : String) : Element = Element();
    fun path() : Element = Element();

    fun ellipse(x : Int, y : Int, rx : Int, ry : Int) : Element = Element();
}

NativeFun("Raphael")
fun Raphael(elementId : String, width : Int, height : Int) : Paper = Paper();
NativeFun("Raphael")
fun Raphael(elementId : String, width : Int, height : Int, initFun : Paper.() -> Unit) : Unit {}

NativeClass
val Raphael = object {
    fun getColor() : String = ""
}
