@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import kotlin.test.*
import kotlin.native.Platform

fun main(args: Array<String>) {
    // Remove path and extension (.kexe or .exe)
    val programFileName = Platform.programName.substringAfterLast("/").substringBeforeLast(".")

    assertEquals("program_name", programFileName)
}
