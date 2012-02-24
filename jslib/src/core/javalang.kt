package java.lang

import java.util.Iterator;
import js.library

library
trait Iterable<T> {
    fun iterator() : java.util.Iterator<T> = js.noImpl
}

library("splitString")
public fun String.split(regex : String) : Array<String> = js.noImpl

