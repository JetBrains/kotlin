/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.utilities

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.delimiter
import kotlinx.cli.required

fun transformSwiftExportArgsIntoKotilnNativeCArgs(args: Array<String>): Array<String> {
    val arguments = SwiftExportArguments()
    arguments.argParser.parse(args)

    val swiftExportCompilerPluginPath = arguments.swiftExportCompilerPluginPath
    val outputDir = arguments.outputDirectory
    val named = arguments.resultedFileName
    val sources = arguments.sources
    return arrayOf(
            "-Xswift-export-run",
            "-language-version", "2.0",
            "-Xcompiler-plugin=$swiftExportCompilerPluginPath=output_dir=$outputDir".let {
                if (named.isNullOrBlank()) {
                    it
                } else {
                    "$it=named=$named"
                }
            },
            "-p", "library",
    ) + sources
}

private class SwiftExportArguments {
    val argParser: ArgParser = ArgParser("kotlin-native-swift-export", prefixStyle = ArgParser.OptionPrefixStyle.JVM)

    val swiftExportCompilerPluginPath by argParser
            .option(
                    ArgType.String,
                    description = "Path to compiled swift-export.embeddable plugin. May not be null.")
            .required()
    val outputDirectory by argParser
            .option(
                    ArgType.String,
                    description = "Path for output")
            .required()
    val sources by argParser
            .option(
                    ArgType.String,
                    description = "Paths for input sources")
            .required()
            .delimiter(",")
    val resultedFileName by argParser
            .option(
                    ArgType.String,
                    description = "Filename for results")
}
