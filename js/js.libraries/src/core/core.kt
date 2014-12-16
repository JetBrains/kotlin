package kotlin.js

import java.util.*;

native
public val <T> noImpl: T = throw Exception()

// Drop this after KT-2093 will be fixed and restore MutableMap.set in Maps.kt from MapsJVM.kt
/** Provides [] access to maps */
[suppress("BASE_WITH_NULLABLE_UPPER_BOUND")]
native public fun <K, V> MutableMap<K, V>.set(key: K, value: V): V? = noImpl

library("println")
public fun println() {}
library("println")
public fun println(s : Any?) {}
library("print")
public fun print(s : Any?) {}
//TODO: consistent parseInt
native public fun parseInt(s: String, radix: Int = 10): Int = noImpl
library
public fun safeParseInt(s : String) : Int? = noImpl
library
public fun safeParseDouble(s : String) : Double? = noImpl

native public fun js(code: String): dynamic = noImpl