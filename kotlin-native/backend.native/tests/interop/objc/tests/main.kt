@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import kotlin.system.exitProcess
import kotlinx.cinterop.autoreleasepool
import kotlin.native.internal.test.testLauncherEntryPoint

fun main(args: Array<String>) {
    autoreleasepool {
        val exitCode = testLauncherEntryPoint(args)
        // Note: this test runner checks for memory leaks after successful execution, unlike standard one.
        if (exitCode != 0) exitProcess(exitCode)
    }
}