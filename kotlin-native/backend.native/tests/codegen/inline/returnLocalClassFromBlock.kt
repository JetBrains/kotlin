/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.inline.returnLocalClassFromBlock

import kotlin.test.*

inline fun <R> call(block: ()->R): R {
    try {
        return block()
    } finally {
        println("Zzz")
    }
}

@Test fun runTest() {
    call { class Z(); Z() }
}
