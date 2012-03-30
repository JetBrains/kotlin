package js;

import js.native

//TODO: declare using number
native
class MathClass() {
    val PI : Double = js.noImpl;
    fun random() : Double = js.noImpl;
    fun abs(value : Double) : Double = js.noImpl
    fun acos(value : Double) : Double = js.noImpl
    fun asin(value : Double) : Double = js.noImpl
    fun atan(value : Double) : Double = js.noImpl
    fun atan2(x : Double, y : Double) : Double = js.noImpl
    fun cos(value : Double) : Double = js.noImpl
    fun sin(value : Double) : Double = js.noImpl
    fun exp(value : Double) : Double = js.noImpl
    fun max(vararg values : Double) : Double = js.noImpl
    fun max(vararg values : Int) : Int = js.noImpl
    fun min(vararg values : Int) : Int = js.noImpl
    fun min(vararg values : Double) : Double = js.noImpl
    fun sqrt(value : Double) : Double = js.noImpl
    fun tan(value : Double) : Double = js.noImpl
    fun log(value : Double) : Double = js.noImpl
    fun pow(base : Double, exp : Double) : Double = js.noImpl
    fun round(value : Number) : Int = js.noImpl
    fun floor(value : Number) : Int = js.noImpl
    fun ceil(value : Number) : Int = js.noImpl
}

native
val Math = MathClass();