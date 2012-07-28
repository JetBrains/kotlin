package js;

import js.native

//TODO: declare using number
native
public class MathClass() {
    public val PI : Double = js.noImpl;
    public fun random() : Double = js.noImpl;
    public fun abs(value : Double) : Double = js.noImpl
    public fun acos(value : Double) : Double = js.noImpl
    public fun asin(value : Double) : Double = js.noImpl
    public fun atan(value : Double) : Double = js.noImpl
    public fun atan2(x : Double, y : Double) : Double = js.noImpl
    public fun cos(value : Double) : Double = js.noImpl
    public fun sin(value : Double) : Double = js.noImpl
    public fun exp(value : Double) : Double = js.noImpl
    public fun max(vararg values : Double) : Double = js.noImpl
    public fun max(vararg values : Int) : Int = js.noImpl
    public fun min(vararg values : Int) : Int = js.noImpl
    public fun min(vararg values : Double) : Double = js.noImpl
    public fun sqrt(value : Double) : Double = js.noImpl
    public fun tan(value : Double) : Double = js.noImpl
    public fun log(value : Double) : Double = js.noImpl
    public fun pow(base : Double, exp : Double) : Double = js.noImpl
    public fun round(value : Number) : Int = js.noImpl
    public fun floor(value : Number) : Int = js.noImpl
    public fun ceil(value : Number) : Int = js.noImpl
}

native
public val Math: MathClass = MathClass();