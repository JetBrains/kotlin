package js;

import js.library
import js.native
import java.util.*;
import java.lang.*;

native
public val noImpl : Nothing = throw Exception()

/** Provides [] access to maps */
native public fun <K, V> MutableMap<K, V>.set(key: K, value: V): Unit = noImpl

library("println")
public fun println() {}
library("println")
public fun println(s : Any?) {}
library("print")
public fun print(s : Any?) {}
//TODO: consistent parseInt
library("parseInt")
public fun parseInt(s : String) : Int = js.noImpl
library
public fun safeParseInt(s : String) : Int? = js.noImpl
library
public fun safeParseDouble(s : String) : Double? = js.noImpl