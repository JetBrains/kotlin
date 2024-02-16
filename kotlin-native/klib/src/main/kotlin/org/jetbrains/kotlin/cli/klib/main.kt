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
        val command = KlibToolCommand(output, args)

        try {
            when (args.commandName) {
                "dump-abi" -> command.dumpAbi()
                "dump-ir" -> command.dumpIr()
                "dump-ir-signatures" -> command.dumpIrSignatures()
                "dump-metadata" -> command.dumpMetadata()
                "dump-metadata-signatures" -> command.dumpMetadataSignatures()
                "contents" -> command.contents()
                "signatures" -> command.signatures()
                "info" -> command.info()
                "install" -> command.install()
                "remove" -> command.remove()
                else -> output.logError("Unknown command: ${args.commandName}")
            }
        } catch (t: Throwable) {
            output.logErrorWithStackTrace(t)
        }
    }

    if (output.hasErrors)
        exitProcess(1)
}
