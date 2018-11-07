/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal.test

import kotlin.system.exitProcess
import kotlin.native.concurrent.*

@ThreadLocal
private val _generatedSuites = mutableListOf<TestSuite>()

public fun registerSuite(suite: TestSuite): Unit {
    _generatedSuites.add(suite)
}

fun testLauncherEntryPoint(args: Array<String>): Int {
    return TestRunner(_generatedSuites, args).run()
}

fun main(args: Array<String>) {
    exitProcess(testLauncherEntryPoint(args))
}

fun worker(args: Array<String>) {
    val worker = Worker.start()
    val result = worker.execute(TransferMode.SAFE, { args.freeze() }) {
        it -> testLauncherEntryPoint(it)
    }.result
    worker.requestTermination().result
    exitProcess(result)
}
