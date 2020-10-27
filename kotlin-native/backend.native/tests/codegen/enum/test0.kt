/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.enum.test0

import kotlin.test.*

val TOP_LEVEL = 5

enum class MyEnum(value: Int) {
    VALUE(TOP_LEVEL)
}

@Test fun runTest() {
    println(MyEnum.VALUE.toString())
}
