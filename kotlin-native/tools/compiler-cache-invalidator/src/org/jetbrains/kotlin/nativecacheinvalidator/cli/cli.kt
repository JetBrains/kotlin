/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:JvmName("NativeCacheInvalidatorCLI")

package org.jetbrains.kotlin.nativecacheinvalidator.cli

import org.jetbrains.kotlin.konan.target.buildDistribution
import org.jetbrains.kotlin.nativecacheinvalidator.invalidateStaleCaches
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.system.exitProcess

private data class Args(
        val dist: String,
        val dataDir: String?,
        val verbose: Boolean,
)

private fun usage() {
    println(
            """
        |USAGE: <native-cache-invalidator-cli>
        |            --dist=<path to distribution directory>
        |            [--data-dir=<path to data directory>]
        |            [-v|--verbose] # enable verbose logging
    """.trimMargin()
    )
}

private fun Array<String>.parse(): Args {
    var dist: String? = null
    var dataDir: String? = null
    var verbose: Boolean = false
    forEach { arg ->
        when {
            arg.startsWith("--dist") -> dist = arg.replace("--dist=", "")
            arg.startsWith("--data-dir") -> dataDir = arg.replace("--data-dir=", "")
            arg == "-v" || arg == "--verbose" -> verbose = true
            else -> {
                error("Unknown argument `$arg`.")
            }
        }
    }
    return Args(
            dist ?: error("--dist=<...> must be specified"),
            dataDir,
            verbose
    )
}

private fun run(args: Args): Nothing {
    val logger = Logger.getLogger("NativeCacheInvalidator")
    logger.level = if (args.verbose) Level.INFO else Level.WARNING
    context(logger) {
        buildDistribution(args.dist, konanDataDir = args.dataDir).invalidateStaleCaches()
    }
    exitProcess(0)
}

fun main(args: Array<String>): Unit = run(try {
    args.parse()
} catch (e: Exception) {
    e.message?.let { println(it) }
    usage()
    exitProcess(1)
})