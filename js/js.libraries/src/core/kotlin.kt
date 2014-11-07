package kotlin

import java.util.*

library
public fun <T> array(vararg value : T): Array<T> = noImpl

// "constructors" for primitive types array

library
public fun doubleArray(vararg content : Double): DoubleArray    = noImpl

library
public fun floatArray(vararg content : Float): FloatArray       = noImpl

library
public fun longArray(vararg content : Long): LongArray          = noImpl

library
public fun intArray(vararg content : Int): IntArray             = noImpl

library
public fun charArray(vararg content : Char): CharArray          = noImpl

library
public fun shortArray(vararg content : Short): ShortArray       = noImpl

library
public fun byteArray(vararg content : Byte): ByteArray          = noImpl

library
public fun booleanArray(vararg content : Boolean): BooleanArray = noImpl

library("copyToArray")
public fun <reified T> Collection<T>.copyToArray(): Array<T> = noImpl
