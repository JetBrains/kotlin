package java.util.Collections

import java.lang.*
import java.util.*

library("collectionsMax")
public fun max<T>(col : Collection<T>, comp : Comparator<T>) : T = js.noImpl

library("collectionsSort")
public fun <T> sort(list: MutableList<T>): Unit = js.noImpl

library("collectionsSort")
public fun <T> sort(list: MutableList<T>, comparator: java.util.Comparator<T>): Unit = js.noImpl