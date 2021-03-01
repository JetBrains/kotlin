/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.enum.values

import kotlin.test.*

enum class E {
    E3,
    E1,
    E2
}

@Test fun runTest() {
    println(E.values()[0].toString())
    println(E.values()[1].toString())
    println(E.values()[2].toString())
    println(enumValues<E>()[0].toString())
    println(enumValues<E>()[1].toString())
    println(enumValues<E>()[2].toString())
}