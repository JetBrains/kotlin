package raphael

import js.*;
import js.*;

native
open class Element() {

    fun attr(name : String, value : Any?) : Element = this
    fun attr(nameToValue : Json) : Element = this

    fun click(handler : Element.()-> Unit) = this;

    fun mouseover(handler : Element.() -> Unit) = this
    fun mouseout(handler : Element.() -> Unit) = this


    fun getTotalLength() : Double = 0.0
    fun getPointAtLength(length : Double) : Point = js.noImpl

    fun animate(params : Json, ms : Int, callback : ()-> Unit) : Element = js.noImpl
    fun animate(params : Json, ms : Int, s : String) : Element = js.noImpl
    fun animate(params : Json, ms : Int) : Element = js.noImpl
    //fun mouse
}

native
class Set() : Element() {
    fun push(el : Element) = js.noImpl
}

native
class Paper() {
    fun path(pathString : String) : Element = Element();
    fun path() : Element = Element();

    fun ellipse(x : Int, y : Int, rx : Int, ry : Int) : Element = js.noImpl
    fun circle(x : Int, y : Int, r : Int) : Element = js.noImpl
    fun set() : Set = js.noImpl

    val customAttributes : Json = Json();
}

native("Raphael")
fun Raphael(elementId : String, width : Int, height : Int) : Paper = Paper();
native("Raphael")
fun Raphael(elementId : String, width : Int, height : Int, initFun : Paper.() -> Unit) : Unit = js.noImpl

native
fun getColor() : Color = js.noImpl
native
fun resetColors() = js.noImpl

native
class Color() {
}

native
class Point() {
    val x = 0
    val y = 0
    val alpha = 0
}
