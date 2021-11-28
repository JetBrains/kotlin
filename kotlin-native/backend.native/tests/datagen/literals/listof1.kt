/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package datagen.literals.listof1

import kotlin.test.*

@Test fun runTest() {
    val list = foo()
    println(list === foo())
    println(list.toString())
}

fun foo(): List<String> {
    return listOf("a", "b", "c")
}