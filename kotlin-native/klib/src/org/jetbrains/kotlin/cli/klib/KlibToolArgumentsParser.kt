/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.library.KotlinIrSignatureVersion
import org.jetbrains.kotlin.library.KotlinLibrary

internal class KlibToolArgumentsParser(private val output: KlibToolOutput) {
    fun parseArguments(rawArgs: Array<String>): KlibToolArgumentsParserResult {
        if (rawArgs.size < 2) {
            printUsage()
            return KlibToolArgumentsParserResult.UsagePrinted
        }

        val command = CliCommand.parseOrNull(rawArgs[0])
        if (command == null) {
            output.logError("Unknown command: ${rawArgs[0]}")
            return KlibToolArgumentsParserResult.Error
        }

        val library = loadKlib(rawArgs[1], output)
                ?: return KlibToolArgumentsParserResult.Error

        val parsedOptions: Map<CliOption, List<String>> = parseOptions(rawArgs.drop(2).toTypedArray<String>())
            ?.entries
            ?.map { [option, values] ->
                val knownOption = CliOption.parseOrNull(option)
                if (knownOption == null) {
                    output.logError("Unknown option: $option")
                    return KlibToolArgumentsParserResult.Error
                }
                if (command !in knownOption.applicableTo) {
                    output.logError("Option $option is not applicable to command $command")
                    return KlibToolArgumentsParserResult.Error
                }
                knownOption to values
            }?.toMap()
            ?: return KlibToolArgumentsParserResult.Error

        val signatureVersion = parsedOptions[CliOption.SIGNATURE_VERSION]?.last()?.let { rawSignatureVersion ->
            rawSignatureVersion.toIntOrNull()?.let(::KotlinIrSignatureVersion) ?: run {
                output.logError("Invalid signature version: $rawSignatureVersion")
                return KlibToolArgumentsParserResult.Error
            }
        }

        if (signatureVersion != null && signatureVersion !in KotlinIrSignatureVersion.CURRENTLY_SUPPORTED_VERSIONS) {
            output.logError("Unsupported signature version: ${signatureVersion.number}")
            return KlibToolArgumentsParserResult.Error
        }

        val dumpMetadataTestMode = parsedOptions[CliOption.DUMP_METADATA_TEST_MODE]?.last()?.let { modeName ->
            MetadataDumpMode.parseOrNull(modeName) ?: run {
                output.logError("Invalid metadata dump mode: $modeName")
                return KlibToolArgumentsParserResult.Error
            }
        }

        return KlibToolArgumentsParserResult.ParsedArguments(
                command = command,
                library = library,
                onlyTopLevelSignatures = parsedOptions[CliOption.ONLY_TOP_LEVEL_SIGNATURES]?.last()?.toBoolean() == true,
                signatureVersion,
                dumpMetadataTestMode = dumpMetadataTestMode,
                relativePathBases = parsedOptions[CliOption.RELATIVE_PATH_BASE] ?: emptyList(),
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

            repeat(DOUBLE_INDENT) { append(' ') }
            option.applicableTo.sorted().joinTo(this, prefix = "Supported in commands: ", postfix = "\n")
        }
    }

    companion object {
        private const val SINGLE_INDENT = 4
        private const val DOUBLE_INDENT = SINGLE_INDENT * 2
    }
}

internal sealed interface KlibToolArgumentsParserResult {
    object Error : KlibToolArgumentsParserResult
    object UsagePrinted : KlibToolArgumentsParserResult

    class ParsedArguments(
            val command: CliCommand,
            val library: KotlinLibrary,
            val onlyTopLevelSignatures: Boolean,
            val signatureVersion: KotlinIrSignatureVersion?,
            val dumpMetadataTestMode: MetadataDumpMode?,
            val relativePathBases: List<String>,
    ) : KlibToolArgumentsParserResult
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

    override fun toString() = commandName

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

        override val applicableTo get() = setOf(CliCommand.DUMP_ABI, CliCommand.DUMP_IR_SIGNATURES, CliCommand.DUMP_METADATA_SIGNATURES)
    },

    ONLY_TOP_LEVEL_SIGNATURES {
        override val hintOnValues = "{true|false}"
        override val description = "Dump IR signatures of only top-level declarations."
        override val applicableTo get() = setOf(CliCommand.DUMP_IR_SIGNATURES)
    },

    RELATIVE_PATH_BASE {
        override val hintOnValues = null

        override val description = """
            A file path prefix to be removed from all paths to render them as relative paths.
            Note: By repeating this option multiple times it's possible to specify multiple prefixes.
        """.trimIndent()

        override val applicableTo get() = setOf(CliCommand.DUMP_IR)
    },

    /**
     * This is an option that allows running the "dump-metadata" command in one of the special "test modes" (see [MetadataDumpMode]).
     * This is essentially helpful for tests, which rely on the stable command output.
     *
     * NOTE: This option is not supposed to be advertised in KLIB tool's "usage" output.
     */
    DUMP_METADATA_TEST_MODE(isPrivate = true) {
        override val applicableTo get() = setOf(CliCommand.DUMP_METADATA)
    },
    ;

    open val hintOnValues: String?
        get() = error("No hint on values available for option $optionName")

    open val description: String
        get() = error("No description available for option $optionName")

    val optionName: String
        get() = "-" + name.replace("_", "-").lowercase()

    abstract val applicableTo: Set<CliCommand>

    override fun toString() = optionName

    companion object {
        fun parseOrNull(optionName: String): CliOption? = entries.firstOrNull { it.optionName == optionName }
    }
}

internal enum class MetadataDumpMode(val modeName: String?) {
    /** The default dump mode. */
    DEFAULT(null),

    /**
     * - empty package fragments are removed
     * - package fragments with the same package FQN are merged
     * - declarations are sorted in alphabetical order
     */
    COMPACT_WITH_STABLE_ORDER("compact-with-stable-order"),

    /**
     * - empty package fragments are removed
     * - package fragments with the same package FQN are merged
     * - declarations are sorted in alphabetical order
     */
    ULTRACOMPACT_WITH_STABLE_ORDER("ultracompact-with-stable-order"),
    ;

    override fun toString() = modeName ?: "<no test mode>"

    companion object {
        fun parseOrNull(modeName: String?): MetadataDumpMode? = MetadataDumpMode.entries.firstOrNull { it.modeName == modeName }
    }
}
