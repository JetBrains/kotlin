@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import kotlin.native.Platform

fun main(args: Array<String>) {
    // Remove path (using both unix/win path seperators) and extension (.kexe or .exe)
    val programFileName = Platform.programName?.substringAfterLast("/")?.substringAfterLast("\\")?.substringBeforeLast(".")

    println("programName: $programFileName")
    println("args: ${args.joinToString()}")
}