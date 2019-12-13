/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cli

import org.jetbrains.kotlin.descriptors.commonizer.konan.NativeDistributionCommonizer
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import kotlin.system.exitProcess
import kotlin.time.ExperimentalTime

@ExperimentalTime
fun main(args: Array<String>) {
    if (args.isEmpty()) printUsageAndExit()

    val parsedArgs = parseArgs(args, ::printUsageAndExit)

    val repository = parsedArgs["-repository"]?.firstOrNull()?.let(::File) ?: printUsageAndExit("repository not specified")

    val targets = with(HostManager()) {
        val targetNames = parsedArgs["-target"]?.toSet() ?: printUsageAndExit("no targets specified")
        targetNames.map { targetName ->
            targets[targetName] ?: printUsageAndExit("unknown target name: $targetName")
        }
    }

    val destination = parsedArgs["-output"]?.firstOrNull()?.let(::File) ?: printUsageAndExit("output not specified")

    NativeDistributionCommonizer(
        repository = repository,
        targets = targets,
        destination = destination,
        handleError = ::printErrorAndExit,
        log = ::println
    ).run()
}

private fun printUsageAndExit(errorMessage: String? = null): Nothing {
    if (errorMessage != null) {
        println("Error: $errorMessage")
        println()
    }

    println("Usage: commonizer <options>")
    println("where possible options include:")
    println("\t-repository <path>\tWork with the specified Kotlin/Native repository")
    println("\t-target <name>\t\tAdd hardware target to commonization")
    println("\t-output <path>\t\tDestination of commonized KLIBs")
    println()

    exitProcess(if (errorMessage != null) 1 else 0)
}
