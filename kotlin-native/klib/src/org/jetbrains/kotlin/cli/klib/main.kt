/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:JvmName("Main")

package org.jetbrains.kotlin.cli.klib

import kotlin.system.exitProcess

/**
 * This entry point is used in various KLIB dumping tests: dumping IR, dumping metadata, dumping signatures, etc.
 */
@Suppress("unused")
fun exec(stdout: Appendable, stderr: Appendable, args: Array<String>): Int {
    return execImpl(KlibToolOutput(stdout, stderr), args)
}

fun main(args: Array<String>) {
    val exitCode = execImpl(KlibToolOutput(stdout = System.out, stderr = System.err), args)
    if (exitCode != 0) exitProcess(exitCode)
}

private fun execImpl(output: KlibToolOutput, rawArgs: Array<String>): Int {
    val args = KlibToolArgumentsParser(output).parseArguments(rawArgs)

    if (args != null) {
        val command = when (args.commandName) {
            "dump-abi" -> DumpAbi(output, args)
            "dump-ir" -> DumpIr(output, args)
            "dump-ir-signatures" -> DumpIrSignatures(output, args)
            "dump-metadata" -> DumpMetadata(output, args)
            "dump-metadata-signatures" -> DumpMetadataSignatures(output, args)
            "contents" -> LegacyContents(output, args)
            "signatures" -> LegacySignatures(output, args)
            "info" -> Info(output, args)
            "install" -> LegacyInstall(output, args)
            "remove" -> LegacyRemove(output, args)
            else -> {
                output.logError("Unknown command: ${args.commandName}")
                null
            }
        }

        try {
            command?.execute()
        } catch (t: Throwable) {
            output.logErrorWithStackTrace(t)
        }
    }

    return if (output.hasErrors) 1 else 0
}
