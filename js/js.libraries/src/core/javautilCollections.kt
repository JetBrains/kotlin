package java.util.Collections

import java.lang.*
import java.util.*

library("collectionsMax")
public fun max<T>(col : Collection<T>, comp : Comparator<in T>) : T = noImpl

library("collectionsSort")
public fun <T> sort(list: MutableList<T>): Unit = noImpl

library("collectionsSort")
public fun <T> sort(list: MutableList<T>, comparator: java.util.Comparator<in T>): Unit = noImpl
