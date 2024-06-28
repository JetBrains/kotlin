/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)
package kotlin.test.tests

import kotlin.test.*
import kotlin.native.internal.test.*

@Test
fun test() {
    println("test")
}

fun main(args: Array<String>) {
    println("Custom main")
    testLauncherEntryPoint(args)
}