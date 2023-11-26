// OUTPUT_DATA_FILE: catch4.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun box(): String {
    try {
        println("Before")
        throw Error("Error happens")
        println("After")
    } catch (e: Exception) {
        println("Caught Exception")
    } catch (e: Error) {
        println("Caught Error")
    } catch (e: Throwable) {
        println("Caught Throwable")
    }

    println("Done")

    return "OK"
}
