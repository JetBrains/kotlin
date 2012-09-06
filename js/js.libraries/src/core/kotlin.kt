package kotlin

import java.util.*

library("comparator")
public fun comparator<T>(f : (T, T) -> Int): Comparator<T> = js.noImpl

public fun <T> array(vararg value: T): Array<T> = js.noImpl

// "constructors" for primitive types array
public fun doubleArray(vararg content : Double): DoubleArray    = js.noImpl

public fun floatArray(vararg content : Float): FloatArray       = js.noImpl

public fun longArray(vararg content : Long): LongArray          = js.noImpl

public fun intArray(vararg content : Int): IntArray             = js.noImpl

public fun charArray(vararg content : Char): CharArray          = js.noImpl

public fun shortArray(vararg content : Short): ShortArray       = js.noImpl

public fun byteArray(vararg content : Byte): ByteArray          = js.noImpl

public fun booleanArray(vararg content : Boolean): BooleanArray = js.noImpl

