package kotlin

import java.util.*

library
public fun <T> arrayOf(vararg value : T): Array<T> = noImpl

// "constructors" for primitive types array

library
public fun doubleArrayOf(vararg content : Double): DoubleArray    = noImpl

library
public fun floatArrayOf(vararg content : Float): FloatArray       = noImpl

library
public fun longArrayOf(vararg content : Long): LongArray          = noImpl

library
public fun intArrayOf(vararg content : Int): IntArray             = noImpl

library
public fun charArrayOf(vararg content : Char): CharArray          = noImpl

library
public fun shortArrayOf(vararg content : Short): ShortArray       = noImpl

library
public fun byteArrayOf(vararg content : Byte): ByteArray          = noImpl

library
public fun booleanArrayOf(vararg content : Boolean): BooleanArray = noImpl

library("copyToArray")
public fun <reified T> Collection<T>.copyToArray(): Array<T> = noImpl
