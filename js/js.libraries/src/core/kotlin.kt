package kotlin

import java.util.*

library("comparator")
public fun comparator<T>(f : (T, T) -> Int) : Comparator<T> = js.noImpl

library("array")
public fun <T> array(vararg value: T): Array<T> = js.noImpl

