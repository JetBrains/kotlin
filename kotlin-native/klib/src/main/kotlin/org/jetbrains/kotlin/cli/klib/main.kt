/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.cli.klib

// TODO: Extract `library` package as a shared jar?
import kotlin.system.exitProcess

fun main(rawArgs: Array<String>) {
    val output = KlibToolOutput(stdout = System.out, stderr = System.err)

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

    if (output.hasErrors)
        exitProcess(1)
}
