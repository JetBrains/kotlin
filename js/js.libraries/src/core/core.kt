package js;

import js.library
import js.native
import java.util.*;
import java.lang.*;

native
public val noImpl : Nothing = throw Exception()

/** Provides [] access to maps */
native public fun <K, V> MutableMap<K, V>.set(key: K, value: V): Unit = noImpl

library("hashCode")
public fun Any.hashCode() : Int = js.noImpl

library("hashNumberImpl")
public fun Number.hashCode() : Int = js.noImpl

library("hashStringImpl")
public fun String.hashCode() : Int = js.noImpl

library("hashStringImpl")
public fun Char.hashCode() : Int = js.noImpl

library("hashArrayImpl")
public fun <T> Array<T>.hashCode() : Int = js.noImpl

library("hashArrayImpl")
public fun ByteArray.hashCode() : Int = js.noImpl

library("hashArrayImpl")
public fun ShortArray.hashCode() : Int = js.noImpl

library("hashArrayImpl")
public fun IntArray.hashCode() : Int = js.noImpl

library("hashArrayImpl")
public fun LongArray.hashCode() : Int = js.noImpl

library("hashArrayImpl")
public fun FloatArray.hashCode() : Int = js.noImpl

library("hashArrayImpl")
public fun DoubleArray.hashCode() : Int = js.noImpl

library("hashArrayImpl")
public fun CharArray.hashCode() : Int = js.noImpl

library("hashArrayImpl")
public fun BooleanArray.hashCode() : Int = js.noImpl

library("println")
public fun println() {}
library("println")
public fun println(s : Any?) {}
library("print")
public fun print(s : Any?) {}
//TODO: consistent parseInt
native public fun parseInt(s: String, radix: Int = 10): Int = js.noImpl
library
public fun safeParseInt(s : String) : Int? = js.noImpl
library
public fun safeParseDouble(s : String) : Double? = js.noImpl