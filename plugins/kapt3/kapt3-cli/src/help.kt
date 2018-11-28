/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.cli

import org.jetbrains.kotlin.kapt.cli.CliToolOption.Format.*

internal fun printHelp() {
    class OptionToRender(nameArgs: String, val description: String) {
        val nameArgs = nameArgs.trim()
        fun render(width: Int) = "  " + nameArgs + " ".repeat(width - nameArgs.length) + description
    }

    val options = KaptCliOption.values()
        .filter { it.cliToolOption != null }
        .map { OptionToRender(it.nameArgs(), it.description) }

    val optionNameColumnWidth = options.asSequence().map { it.nameArgs.length }.max()!! + 2
    val renderedOptions = options.joinToString("\n|") { it.render(optionNameColumnWidth) }

    val message = """
        |kapt: Run annotation processing over the specified Kotlin source files.
        |Usage: kapt <options> <source files>

        |Options related to annotation processing:
        |$renderedOptions

        |You can also pass all valid Kotlin compiler options.
        |Run 'kotlinc -help' to show them.
    """.trimMargin()

    println(message)
}

private fun KaptCliOption.nameArgs(): String {
    val cliToolOption = this.cliToolOption!!
    return when (cliToolOption.format) {
        FLAG -> cliToolOption.name + "=<true|false>"
        VALUE -> cliToolOption.name + "=" + valueDescription
        KEY_VALUE -> cliToolOption.name + valueDescription
    }
}