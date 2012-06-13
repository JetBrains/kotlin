package js;

import js.library
import js.native
import java.util.*;
import java.lang.*;


native
val noImpl : Nothing = throw Exception();

library("println")
fun println() {}
library("println")
fun println(s : Any?) {}
library("print")
fun print(s : Any?) {}
//TODO: consistent parseInt
library("parseInt")
fun parseInt(s : String) : Int = js.noImpl
library
fun safeParseInt(s : String) : Int? = js.noImpl
library
fun safeParseDouble(s : String) : Double? = js.noImpl