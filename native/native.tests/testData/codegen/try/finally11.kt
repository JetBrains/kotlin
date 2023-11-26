// OUTPUT_DATA_FILE: finally11.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun box(): String {
    try {
        try {
            return "FAIL: try try has unexpetedly returned"
        } catch (e: Error) {
            println("Catch 1")
        } finally {
            println("Finally")
            throw Error()
        }
    } catch (e: Error) {
        println("Catch 2")
    }

    println("Done")

    return "OK"
}
