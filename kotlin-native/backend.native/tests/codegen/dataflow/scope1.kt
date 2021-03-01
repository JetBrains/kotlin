/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.dataflow.scope1

import kotlin.test.*

var b = true

@Test fun runTest() {
    var x = 1
    if (b) {
        var x = 2
    }
    println(x)
}
