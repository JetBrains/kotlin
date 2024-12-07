@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import kotlin.native.Platform
import kotlin.test.*

fun main(args: Array<String>) {
    // Remove path and extension (.kexe or .exe)
     val programFileName = Platform.programName!!.substringAfterLast("/").substringAfterLast("\\").substringBeforeLast(".")

    assertEquals("standalone_entryPoint_programName", programFileName)
}