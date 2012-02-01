package raphael

import js.annotations.*;
import js.*;

Native
open class Element() {

    fun attr(name : String, value : Any?) : Element = this
    fun attr(nameToValue : Json) : Element = this

    fun click(handler : Element.()-> Unit) = this;

    fun mouseover(handler : Element.() -> Unit) = this
    fun mouseout(handler : Element.() -> Unit) = this


    fun getTotalLength() : Double = 0.0
    fun getPointAtLength(length : Double) : Point {}

    fun animate(params : Json, ms : Int, callback : ()-> Unit) : Element {}
    fun animate(params : Json, ms : Int, s : String) : Element {}
    fun animate(params : Json, ms : Int) : Element {}
    //fun mouse
}

Native
class Set() : Element() {
    fun push(el : Element) {}
}

Native
class Paper() {
    fun path(pathString : String) : Element = Element();
    fun path() : Element = Element();

    fun ellipse(x : Int, y : Int, rx : Int, ry : Int) : Element {}
    fun circle(x : Int, y : Int, r : Int) : Element {}
    fun set() : Set {}

    val customAttributes : Json = Json();
}

Native("Raphael")
fun Raphael(elementId : String, width : Int, height : Int) : Paper = Paper();
Native("Raphael")
fun Raphael(elementId : String, width : Int, height : Int, initFun : Paper.() -> Unit) : Unit {}

Native
fun getColor() : Color {}
Native
fun resetColors() {}

Native
class Color() {}

Native
class Point() {
    val x = 0
    val y = 0
    val alpha = 0
}
