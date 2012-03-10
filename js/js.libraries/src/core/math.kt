package js;

import js.native

native
class MathClass() {
    val PI : Double = 1.0;
    fun random() : Double = 0.0;
    fun abs(value : Double) = 0.0
    fun acos(value : Double) = 0.0
    fun asin(value : Double) = 0.0
    fun atan(value : Double) = 0.0
    fun atan2(x : Double, y : Double) = 0.0
    fun cos(value : Double) = 0.0
    fun sin(value : Double) = 0.0
    fun exp(value : Double) = 0.0
    fun max(vararg values : Double) = 0.0
    fun max(vararg values : Int) : Int = js.noImpl
    fun min(vararg values : Int) : Int = js.noImpl
    fun min(vararg values : Double) = 0.0
    fun sqrt(value : Double) = 0.0
    fun tan(value : Double) = 0.0
    fun log(value : Double) = 0.0
    fun pow(base : Double, exp : Double) = 0.0
    fun round(value : Number) = 0
    fun floor(value : Number) = 0
    fun ceil(value : Number) = 0

}

native
val Math = MathClass();