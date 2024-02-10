/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("KaptCli")
package org.jetbrains.kotlin.kapt.cli

import com.intellij.util.PathUtil
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.ArgumentParseErrors
import org.jetbrains.kotlin.cli.common.arguments.preprocessCommandLineArguments
import org.jetbrains.kotlin.cli.common.arguments.validateArguments
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.modules.isAtLeastJava9
import org.jetbrains.kotlin.kapt.cli.CliToolOption.Format.*
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val messageCollector = PrintingMessageCollector(System.err, MessageRenderer.PLAIN_RELATIVE_PATHS, false)

    if (args.isEmpty() || args.contains("-help")) {
        printHelp()
        return
    }

    val kaptTransformed = transformArgs(args.asList(), messageCollector, false)

    if (messageCollector.hasErrors()) {
        exitProcess(ExitCode.COMPILATION_ERROR.code)
    }

    K2JVMCompiler.main(kaptTransformed.toTypedArray())
}

@TestOnly
internal fun transformArgs(args: List<String>, messageCollector: MessageCollector, isTest: Boolean): List<String> {
    val parseErrors = ArgumentParseErrors()
    val kotlincTransformed = preprocessCommandLineArguments(args, lazy { parseErrors })

    val errorMessage = validateArguments(parseErrors)
    if (errorMessage != null) {
        messageCollector.report(CompilerMessageSeverity.ERROR, errorMessage)
        return emptyList()
    }

    return try {
        transformKaptToolArgs(kotlincTransformed, messageCollector, isTest)
    } catch (e: IllegalArgumentException) {
        messageCollector.report(CompilerMessageSeverity.ERROR, e.localizedMessage)
        emptyList()
    }
}

private const val KAPT_COMPILER_PLUGIN_JAR_NAME = "kotlin-annotation-processing.jar"

private fun transformKaptToolArgs(args: List<String>, messageCollector: MessageCollector, isTest: Boolean): List<String> {
    val transformed = mutableListOf<String>()

    if (!isTest) {
        val kaptCompilerPluginFile = findKaptCompilerPlugin()
            ?: throw IllegalStateException("Can't find $KAPT_COMPILER_PLUGIN_JAR_NAME")

        transformed += "-Xplugin=${kaptCompilerPluginFile.absolutePath}"
    }

    var toolsJarPassed = false
    var aptModePassed = false
    var kaptVerboseModePassed = false

    data class Option(val cliToolOption: CliToolOption, val pluginOption: KaptCliOption)

    val cliOptions = KaptCliOption.values().mapNotNull { Option(it.cliToolOption ?: return@mapNotNull null, it) }

    val iterator = args.asIterable().iterator()
    loop@ while (iterator.hasNext()) {
        val arg = iterator.next()
        if (arg == "--") {
            transformed += arg
            iterator.forEach { transformed += it }
            break
        }

        if (arg == "-help") {
            throw IllegalStateException("-help option should be already processed")
        }

        val option = cliOptions.firstOrNull { it.cliToolOption.matches(arg) }
        if (option == null) {
            transformed += arg
            continue
        }

        val transformedOption = option.cliToolOption.transform(arg)

        when (option.pluginOption) {
            KaptCliOption.TOOLS_JAR_OPTION -> {
                // TOOLS_JAR option is not passed as other compiler plugin options.
                // It is only used in kapt-cli, and we add a -Xplugin compiler option instead.

                toolsJarPassed = true
                transformed.add(0, "-Xplugin=$transformedOption")
                continue@loop
            }
            KaptCliOption.APT_MODE_OPTION -> aptModePassed = true
            KaptCliOption.VERBOSE_MODE_OPTION -> kaptVerboseModePassed = true
            KaptCliOption.USE_K2 -> transformed.add("-Xuse-kapt4")
            else -> {}
        }

        transformed += kaptArg(option.pluginOption, transformedOption)
    }

    if (!aptModePassed) {
        val isK2 = "-Xuse-kapt4" in transformed && ("-Xuse-k2" in transformed ||
                transformed.any { it.startsWith("-language-version=2") } ||
                transformed.lastIndexOf("-language-version").takeIf { it >= 0 }
                    ?.let { transformed.getOrNull(it + 1)?.startsWith('2') } == true)

        transformed.addAll(0, kaptArg(KaptCliOption.APT_MODE_OPTION, if (isK2) "stubsAndApt" else "compile"))
    }

    if (!isTest && !isAtLeastJava9() && !areJavacComponentsAvailable() && !toolsJarPassed) {
        val toolsJarFile = findToolsJar()
            ?: argError("'tools.jar' location should be specified (${KaptCliOption.TOOLS_JAR_OPTION.cliToolOption!!.name}=<path>)")
        transformed.add(0, "-Xplugin=" + toolsJarFile.absolutePath)
    }

    if (kaptVerboseModePassed) {
        messageCollector.report(CompilerMessageSeverity.INFO, "Options passed to kotlinc: " + transformed.joinToString(" "))
    }

    return transformed
}

private fun CliToolOption.matches(arg: String) = when (format) {
    FLAG, VALUE -> arg.startsWith(name + "=")
    KEY_VALUE -> arg.startsWith(name + ":")
}

private fun CliToolOption.transform(arg: String): String {
    val optionName = name

    return when (format) {
        FLAG -> {
            fun err(): Nothing = argError("Invalid option format, should be $optionName=true/false")

            if (arg.length < (optionName.length + 2)) err()
            arg.drop(optionName.length + 1).takeIf { it == "true" || it == "false" } ?: err()
        }
        VALUE -> {
            fun err(): Nothing = argError("Invalid option format, should be $optionName=<value>")

            if (arg.length < (optionName.length + 2)) err()
            arg.drop(optionName.length + 1)
        }
        KEY_VALUE -> {
            fun err(): Nothing = argError("Invalid option format, should be $optionName:<key>=<value>")

            if (arg.length < (optionName.length + 3) || arg[optionName.length] != ':') err()
            arg.drop(optionName.length + 1).takeIf { it.contains('=') } ?: err()
        }
    }
}

private fun kaptArg(option: KaptCliOption, value: String): List<String> {
    return listOf("-P", "plugin:" + KaptCliOption.ANNOTATION_PROCESSING_COMPILER_PLUGIN_ID + ":" + option.optionName + "=" + value)
}

private fun argError(text: String): Nothing {
    throw IllegalArgumentException(text)
}

private fun findKaptCompilerPlugin(): File? {
    val pathToThisJar = File(PathUtil.getJarPathForClass(CliToolOption::class.java))
    if (pathToThisJar.extension.lowercase() != "jar") {
        return null
    }

    return File(pathToThisJar.parentFile, KAPT_COMPILER_PLUGIN_JAR_NAME).takeIf { it.exists() }
}