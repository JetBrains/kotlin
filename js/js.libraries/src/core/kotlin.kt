package kotlin

import java.util.*

library("comparator")
public fun comparator<T>(f : (T, T) -> Int): Comparator<T> = js.noImpl

library
public fun <T> array(vararg value : T): Array<T> = js.noImpl

// "constructors" for primitive types array

library
public fun doubleArray(vararg content : Double): DoubleArray    = js.noImpl

library
public fun floatArray(vararg content : Float): FloatArray       = js.noImpl

library
public fun longArray(vararg content : Long): LongArray          = js.noImpl

library
public fun intArray(vararg content : Int): IntArray             = js.noImpl

library
public fun charArray(vararg content : Char): CharArray          = js.noImpl

library
public fun shortArray(vararg content : Short): ShortArray       = js.noImpl

library
public fun byteArray(vararg content : Byte): ByteArray          = js.noImpl

library
public fun booleanArray(vararg content : Boolean): BooleanArray = js.noImpl

library("copyToArray")
public fun <reified T> Collection<T>.copyToArray(): Array<T> = js.noImpl
