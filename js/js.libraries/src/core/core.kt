package js;

import js.library
import js.native
import java.util.*;
import java.lang.*;

native
public val noImpl : Nothing = throw Exception()

// Drop this after KT-2093 will be fixed and restore MutableMap.set in Maps.kt from MapsJVM.kt
/** Provides [] access to maps */
native public fun <K, V> MutableMap<K, V>.set(key: K, value: V): V? = noImpl

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
