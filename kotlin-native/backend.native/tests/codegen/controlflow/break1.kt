/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.controlflow.break1

import kotlin.test.*

@Test fun runTest() {
    loop@ while (true) {
        println("Body")
        break
    }
    println("Done")
}
