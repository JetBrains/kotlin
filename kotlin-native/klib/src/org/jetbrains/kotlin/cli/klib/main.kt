/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:JvmName("Main")

package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.cli.klib.KlibToolArgumentsParserResult.ParsedArguments
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
    when (val args = KlibToolArgumentsParser(output).parseArguments(rawArgs)) {
        KlibToolArgumentsParserResult.Error -> return 1
        KlibToolArgumentsParserResult.UsagePrinted -> return 0
        is ParsedArguments -> {
            val command = when (args.command) {
                CliCommand.DUMP_ABI -> DumpAbi(output, args)
                CliCommand.DUMP_IR -> DumpIr(output, args)
                CliCommand.DUMP_IR_INLINABLE_FUNCTIONS -> DumpIrInlinableFunctions(output, args)
                CliCommand.DUMP_SIGNATURES -> DumpSignatures(output, args)
                CliCommand.DUMP_METADATA -> DumpMetadata(output, args)
                CliCommand.INFO -> Info(output, args)
            }

            try {
                command.execute()
            } catch (t: Throwable) {
                output.logErrorWithStackTrace(t)
            }

            return if (output.hasErrors) 1 else 0
        }
    }
}
