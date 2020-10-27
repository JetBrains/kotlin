/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.boxing.boxing0

import kotlin.test.*

class Box<T>(t: T) {
    var value = t
}

@Test fun runTest() {
    val box: Box<Int> = Box<Int>(17)
    println(box.value)
}

