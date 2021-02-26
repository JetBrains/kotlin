/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import platform.Foundation.*
import platform.objc.*
import kotlin.test.*

import kotlinx.cinterop.*
import platform.posix.*
import kotlin.system.exitProcess

fun exc_handler(x: Any?) : Unit {
    println("Uncaught exception handler")
    println(x.toString())
    exitProcess(0)
}

fun main() {
    // This does not work anymore, as NSException propagated as ForeignException
    // so we got `Uncaught Kotlin exception` instead. Use normal try/catch.
    objc_setUncaughtExceptionHandler(staticCFunction(::exc_handler))

    try {
        println(NSJSONSerialization())
    } catch (e: Exception) { // ForeignException expected
        println(e)
    }
}
