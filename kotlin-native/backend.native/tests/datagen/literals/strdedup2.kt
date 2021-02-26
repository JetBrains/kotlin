/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package datagen.literals.strdedup2

import kotlin.test.*

@Test fun runTest() {
    val str1 = ""
    val str2 = "hello".subSequence(2, 2)
    println(str1 == str2)
    println(str1 === str2)
}
