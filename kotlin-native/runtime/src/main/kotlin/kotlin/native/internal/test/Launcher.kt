/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal.test

import kotlin.system.exitProcess
import kotlin.native.concurrent.*

@ThreadLocal
private object GeneratedSuites {
   val suites = mutableListOf<TestSuite>()
   fun add(suite: TestSuite) = suites.add(suite)
}

public fun registerSuite(suite: TestSuite): Unit {
    GeneratedSuites.add(suite)
}

fun testLauncherEntryPoint(args: Array<String>): Int {
    return TestRunner(GeneratedSuites.suites, args).run()
}

fun main(args: Array<String>) {
    val exitCode = testLauncherEntryPoint(args)
    if (exitCode != 0) {
        exitProcess(exitCode)
    }
}

fun worker(args: Array<String>) {
    val worker = Worker.start()
    val exitCode = worker.execute(TransferMode.SAFE, { args.freeze() }) {
        it -> testLauncherEntryPoint(it)
    }.result
    worker.requestTermination().result
    if (exitCode != 0) {
        exitProcess(exitCode)
    }
}

fun mainNoExit(args: Array<String>) {
    testLauncherEntryPoint(args)
}
