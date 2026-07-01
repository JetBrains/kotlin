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

        val extraArgs: Map<CliOption, List<String>> = parseOptions(rawArgs.drop(2).toTypedArray<String>())
            ?.entries
            ?.mapNotNull { [option, values] ->
                val knownOption = CliOption.parseOrNull(option)
                if (knownOption == null) {
                    output.logWarning("Unknown option: $option")
                    return@mapNotNull null
                }
                knownOption to values
            }?.toMap()
            ?: return null

        val signatureVersion = extraArgs[CliOption.SIGNATURE_VERSION]?.last()?.let { rawSignatureVersion ->
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
            libraryPath = rawArgs[1],
            printSignatures = extraArgs[CliOption.PRINT_SIGNATURES]?.last()?.toBoolean() == true,
            onlyTopLevelSignatures = extraArgs[CliOption.ONLY_TOP_LEVEL_SIGNATURES]?.last()?.toBoolean() == true,
            signatureVersion,
            testMode = extraArgs[CliOption.TEST_MODE]?.last()?.toBoolean() == true,
            absolutePathPrefixes = extraArgs[CliOption.ABSOLUTE_PATH_PREFIX] ?: emptyList(),
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

    private fun printUsage(): Unit = with(output.stderr) {
        appendLine("Usage: klib <command> <library path> [<options>]")
        appendLine()
        appendLine("where the commands are:")

        CliCommand.entries.forEachIndexed { index, command ->
            if (index > 0) appendLine()

            repeat(SINGLE_INDENT) { append(' ') }
            appendLine(command.commandName)

            command.description.lines().forEach { descriptionLine ->
                repeat(DOUBLE_INDENT) { append(' ') }
                appendLine(descriptionLine)
            }
        }

        appendLine()
        appendLine("and the options are:")

        CliOption.entries.filterNot { it.isPrivate }.forEachIndexed { index, option ->
            if (index > 0) appendLine()

            repeat(SINGLE_INDENT) { append(' ') }
            append(option.optionName)

            when (val hintOnValues = option.hintOnValues) {
                null -> appendLine()
                else -> append(' ').appendLine(hintOnValues)
            }

            option.description.lines().forEach { descriptionLine ->
                repeat(DOUBLE_INDENT) { append(' ') }
                appendLine(descriptionLine)
            }
        }
    }

    companion object {
        private const val SINGLE_INDENT = 4
        private const val DOUBLE_INDENT = SINGLE_INDENT * 2
    }
}

internal enum class CliCommand(val description: String) {
    INFO(description = "Print general information about the library."),

    DUMP_ABI(
            description = """
                Dump the ABI snapshot of the library. Each line in the snapshot corresponds exactly to one
                declaration. Whenever an ABI-incompatible change happens to a declaration, this should
                be visible in the corresponding line of the snapshot.
            """.trimIndent()
    ),

    DUMP_IR(
            description = """
                Dump the intermediate representation (IR) of declarations in the library.
                The output of this command is intended to be used for debugging purposes only.
            """.trimIndent()
    ),

    DUMP_IR_SIGNATURES(
            description = """
                Dump IR signatures of all non-private declarations in the library and all non-private
                declarations consumed by this library (as two separate lists). This command relies
                purely on the data in IR.
            """.trimIndent()
    ),

    DUMP_IR_INLINABLE_FUNCTIONS(
            description = """
                Dump the intermediate representation (IR) of inlinable functions in the library.
                The output of this command is intended to be used for debugging purposes only.
            """.trimIndent()
    ),

    DUMP_METADATA(
            description = """
                Dump the metadata of all declarations in the library.
                The output of this command intended to be used for debugging purposes only.
            """.trimIndent()
    ),

    DUMP_METADATA_SIGNATURES(
            description = """
                Dump IR signatures of all non-private declarations in the library. Note, that this command
                renders the signatures based on the library metadata. This is different from
                "dump-ir-signatures", which renders signatures based on the IR. On practice,
                in most cases there is no difference between output of these two commands. However,
                if IR transforming compiler plugins (such as Compose) were used during compilation
                of the library, there would be different signatures for patched declarations.
            """.trimIndent()
    ),
    ;

    val commandName: String
        get() = name.replace("_", "-").lowercase()

    companion object {
        fun parseOrNull(commandName: String): CliCommand? = entries.firstOrNull { it.commandName == commandName }
    }
}

private enum class CliOption(val isPrivate: Boolean = false) {
    SIGNATURE_VERSION {
        override val hintOnValues
            get() = "{${KotlinIrSignatureVersion.CURRENTLY_SUPPORTED_VERSIONS.joinToString("|") { it.number.toString() }}}"

        override val description = """
                Render IR signatures of a specific version. By default, the most up-to-date signature version
                that is supported in the library is used.
            """.trimIndent()
    },

    ONLY_TOP_LEVEL_SIGNATURES {
        override val hintOnValues = "{true|false}"
        override val description = "Dump IR signatures of only top-level declarations. Applicable only to the \"dump-ir-signatures\" command."
    },

    PRINT_SIGNATURES {
        override val hintOnValues = "{true|false}"
        override val description = "Print IR signature for every declaration. Applicable only to the \"dump-metadata\" command."
    },

    /**
     * A file path prefix to be removed from full paths to render relative paths, thus making dumps reproducible.
     */
    ABSOLUTE_PATH_PREFIX(isPrivate = true),

    /**
     * This is an option that allows running the commands that support it in a special "test mode".
     * The "test mode" means (but not limited to) that a command may, for example, sort the output
     * which is unsorted by default, and this way guarantee stable output. This is essentially helpful
     * for tests, which rely on the command output.
     *
     * NOTE: This option is not supposed to be advertised in KLIB tool's "usage info".
     */
    TEST_MODE(isPrivate = true),
    ;

    open val hintOnValues: String?
        get() = error("No hint on values available for option $optionName")

    open val description: String
        get() = error("No description available for option $optionName")

    val optionName: String
        get() = "-" + name.replace("_", "-").lowercase()

    companion object {
        fun parseOrNull(optionName: String): CliOption? = entries.firstOrNull { it.optionName == optionName }
    }
}
