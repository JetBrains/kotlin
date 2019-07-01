/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections.js

fun testSize(): Int {
    val a1 = arrayOf<String>()
    val a2 = arrayOf("foo")
    val a3 = arrayOf("foo", "bar")

    return a1.size + a2.size + a3.size
}

fun testToListToString(): String {
    val a1 = arrayOf<String>()
    val a2 = arrayOf("foo")
    val a3 = arrayOf("foo", "bar")

    return a1.toList().toString() + "-" + a2.toList().toString() + "-" + a3.toList().toString()
}
