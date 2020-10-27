/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.enum.lambdaInDefault

import kotlin.test.*

enum class Zzz(val value: String.() -> Int = {
    length
}) {
    Q()
}

@Test fun runTest() {
    println(Zzz.Q)
}