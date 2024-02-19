/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.library.KotlinIrSignatureVersion

internal class KlibToolArgumentsParser(private val output: KlibToolOutput) {
    fun parseArguments(rawArgs: Array<String>): KlibToolArguments? {
        if (rawArgs.size < 2) {
            printUsage()
            return null
        }

        val extraArgs: Map<ExtraOption, List<String>> = parseOptions(rawArgs.drop(2).toTypedArray<String>())
                ?.entries
                ?.mapNotNull { (option, values) ->
                    val knownOption = ExtraOption.parseOrNull(option)
                    if (knownOption == null) {
                        output.logWarning("Unrecognized command-line argument: $option")
                        return@mapNotNull null
                    }
                    knownOption to values
                }?.toMap()
                ?: return null

        val signatureVersion = extraArgs[ExtraOption.SIGNATURE_VERSION]?.last()?.let { rawSignatureVersion ->
            rawSignatureVersion.toIntOrNull()?.let(::KotlinIrSignatureVersion) ?: run {
                output.logError("Invalid signature version: $rawSignatureVersion")
                return null
            }
        }

        if (signatureVersion != null && signatureVersion !in KotlinIrSignatureVersion.CURRENTLY_SUPPORTED_VERSIONS) {
            output.logError("Unsupported signature version: ${signatureVersion.number}")
            return null
        }

        return KlibToolArguments(
                commandName = rawArgs[0],
                libraryNameOrPath = rawArgs[1],
                repository = extraArgs[ExtraOption.REPOSITORY]?.last(),
                printSignatures = extraArgs[ExtraOption.PRINT_SIGNATURES]?.last()?.toBoolean() == true,
                signatureVersion,
                testMode = extraArgs[ExtraOption.INTERNAL_TEST_MODE]?.last()?.toBoolean() == true
        )
    }

    private fun parseOptions(args: Array<String>): Map<String, List<String>>? {
        val options = mutableMapOf<String, MutableList<String>>()
        for (index in args.indices step 2) {
            val key = args[index]
            if (key[0] != '-') {
                output.logError("Expected a flag with initial dash: $key")
                return null
            }
            if (index + 1 == args.size) {
                output.logError("Expected an value after $key")
                return null
            }
            val value = listOf(args[index + 1])
            options[key]?.addAll(value) ?: options.put(key, value.toMutableList())
        }
        return options
    }

    private fun printUsage() {
        output.stderr.appendLine(
                """
                Usage: klib <command> <library> [<option>]

                where the commands are:
                   info                      General information about the library
                   install                   [DEPRECATED] Local KLIB repositories to be dropped soon. See https://youtrack.jetbrains.com/issue/KT-61098
                                               Install the library to the local repository.
                   remove                    [DEPRECATED] Local KLIB repositories to be dropped soon. See https://youtrack.jetbrains.com/issue/KT-61098
                                               Remove the library from the local repository.
                   dump-abi                  Dump the ABI snapshot of the library. Each line in the snapshot corresponds exactly to one
                                               declaration. Whenever an ABI-incompatible change happens to a declaration, this should
                                               be visible in the corresponding line of the snapshot.
                   dump-ir                   Dump the intermediate representation (IR) of all declarations in the library. The output of this
                                               command is intended to be used for debugging purposes only.
                   dump-ir-signatures        Dump IR signatures of all non-private declarations in the library and all non-private declarations
                                               consumed by this library (as two separate lists). This command relies purely on the data in IR.
                   dump-metadata-signatures  Dump IR signatures of all non-private declarations in the library. Note, that this command renders
                                               the signatures based on the library metadata. This is different from "dump-ir-signatures",
                                               which renders signatures based on the IR. On practice, in most cases there is no difference
                                               between output of these two commands. However, if IR transforming compiler plugins
                                               (such as Compose) were used during compilation of the library, there would be different
                                               signatures for patched declarations.
                   signatures                [DEPRECATED] Renamed to "dump-metadata-signatures". Please, use new command name.
                   dump-metadata             Dump the metadata of all declarations in the library. The output of this command is intended
                                               to be used for debugging purposes only.
                   contents                  [DEPRECATED] Reworked and renamed to "dump-metadata". Please, use new command name.

                and the options are:
                   -repository <path>        [DEPRECATED] Local KLIB repositories to be dropped soon. See https://youtrack.jetbrains.com/issue/KT-61098
                                               Work with the specified repository.
                   -signature-version {${KotlinIrSignatureVersion.CURRENTLY_SUPPORTED_VERSIONS.joinToString("|") { it.number.toString() }}}
                                             Render IR signatures of a specific version. By default, the most up-to-date signature version
                                               that is supported in the library is used.
                   -print-signatures {true|false}
                                             Print IR signature for every declaration. Applicable only to "dump-metadata" and "dump-ir" commands.
                """.trimIndent()
        )
    }
}

private enum class ExtraOption(val option: String) {
    REPOSITORY("-repository"),
    PRINT_SIGNATURES("-print-signatures"),
    SIGNATURE_VERSION("-signature-version"),

    /**
     * This is an option that allows running the commands that support it in a special "test mode".
     * The "test mode" means (but not limited to) that a command may, for example, sort the output
     * which is unsorted by default, and this way guarantee stable output. This is essentially helpful
     * for tests, which rely on the command output.
     *
     * NOTE: This option is not supposed to be advertised in KLIB tool's "usage info".
     */
    INTERNAL_TEST_MODE("-test-mode");

    companion object {
        fun parseOrNull(option: String): ExtraOption? = entries.firstOrNull { it.option == option }
    }
}
