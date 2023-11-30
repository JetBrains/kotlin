/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// OUTPUT_DATA_FILE: catch7.out

import kotlin.test.*

fun box(): String {
    try {
        foo()
    } catch (e: Throwable) {
        val message = e.message
        if (message != null) {
            println(message)
        }
    }
    return "OK"
}

fun foo() {
    throw Error("Error happens")
}