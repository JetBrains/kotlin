// OUTPUT_DATA_FILE: finally2.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun box(): String {

    try {
        println("Try")
        throw Error("Error happens")
        println("After throw")
    } catch (e: Error) {
        println("Caught Error")
    } finally {
        println("Finally")
    }

    println("Done")

    return "OK"
}
