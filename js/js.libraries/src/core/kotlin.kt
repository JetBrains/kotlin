package kotlin

import java.util.*

library("comparator")
public fun comparator<T>(f : (T, T) -> Int): Comparator<T> = js.noImpl

library("array")
public fun <T> array(vararg value: T): Array<T> = js.noImpl

// "constructors" for primitive types array
library("array")
public fun doubleArray(vararg content : Double): DoubleArray    = js.noImpl

library("array")
public fun floatArray(vararg content : Float): FloatArray       = js.noImpl

library("array")
public fun longArray(vararg content : Long): LongArray          = js.noImpl

library("array")
public fun intArray(vararg content : Int): IntArray             = js.noImpl

library("array")
public fun charArray(vararg content : Char): CharArray          = js.noImpl

library("array")
public fun shortArray(vararg content : Short): ShortArray       = js.noImpl

library("array")
public fun byteArray(vararg content : Byte): ByteArray          = js.noImpl

library("array")
public fun booleanArray(vararg content : Boolean): BooleanArray = js.noImpl

