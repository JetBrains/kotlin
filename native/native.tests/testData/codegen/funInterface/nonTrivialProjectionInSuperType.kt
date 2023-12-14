/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun <T> foo(comparator: kotlin.Comparator<in T>, a: T, b: T) = comparator.compare(a, b)

fun bar(x: Int, y: Int) = foo<Int> ({ a, b -> a - b}, x, y)

fun box(): String {
    assertTrue(bar(42, 117) < 0)
    return "OK"
}