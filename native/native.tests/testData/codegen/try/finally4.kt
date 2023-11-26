// OUTPUT_DATA_FILE: finally4.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun box(): String {

    try {
        try {
            println("Try")
            throw Error("Error happens")
            println("After throw")
        } catch (e: Error) {
            println("Catch")
            throw Exception()
            println("After throw")
        } finally {
            println("Finally")
        }

        println("After nested try")

    } catch (e: Error) {
        println("Caught Error")
    } catch (e: Exception) {
        println("Caught Exception")
    }

    println("Done")

    return "OK"
}
