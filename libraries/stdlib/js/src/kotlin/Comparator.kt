/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin


public actual interface Comparator<T> {
    @JsName("compare")
    actual fun compare(a: T, b: T): Int
}

public actual inline fun <T> Comparator(crossinline comparison: (a: T, b: T) -> Int): Comparator<T> = object : Comparator<T> {
    override fun compare(a: T, b: T): Int = comparison(a, b)
}
