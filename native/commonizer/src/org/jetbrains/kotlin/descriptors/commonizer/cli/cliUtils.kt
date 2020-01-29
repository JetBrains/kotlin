/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cli

import kotlin.system.exitProcess

internal fun parseArgs(
    args: Array<String>,
    printUsageAndExit: (String) -> Nothing
): Map<String, List<String>> {
    val commandLine = mutableMapOf<String, MutableList<String>>()
    for (index in args.indices step 2) {
        val key = args[index]
        if (key[0] != '-') printUsageAndExit("Expected a flag with initial dash: $key")
        if (index + 1 == args.size) printUsageAndExit("Expected a value after $key")
        val value = args[index + 1]
        commandLine.computeIfAbsent(key) { mutableListOf() }.add(value)
    }
    return commandLine
}

internal fun printErrorAndExit(errorMessage: String): Nothing {
    println("Error: $errorMessage")
    println()

    exitProcess(1)
}
