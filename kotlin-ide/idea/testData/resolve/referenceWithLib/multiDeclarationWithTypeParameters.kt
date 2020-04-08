package test

import dependency.*

fun <Int> f(l: List<Int>) {
    val (e<caret>l1, el2, el3) = l
}

// REF: (for kotlin.collections.List<T> in dependency).component1()

