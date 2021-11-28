/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.memory.throw_cleanup

import kotlin.test.*

@Test fun runTest() {
    foo(false)
    try {
        foo(true)
    } catch (e: Error) {
        println("Ok")
    }
}

fun foo(b: Boolean): Any {
    var result = Any()
    if (b) {
        throw Error()
    }
    return result
}