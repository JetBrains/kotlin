/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.config.CompilerSettings
import org.jetbrains.kotlin.config.additionalArgumentsAsList

/**
 * Turns compiler arguments into the exact list of command line arguments JPS passes to the compiler.
 *
 * This is the single place where "which arguments does JPS pass" is decided, shared by the legacy runner
 * ([JpsKotlinCompilerRunner]) and the Build Tools API one
 * ([org.jetbrains.kotlin.compilerRunner.btapi.JpsBuildToolsApiCompilerRunner]).
 */
internal fun flattenCompilerArguments(
    compilerArgs: CommonCompilerArguments,
    compilerSettings: CompilerSettings?,
): List<String> {
    val allArgs = ArgumentUtils.convertArgumentsToStringList(compilerArgs) +
            (compilerSettings?.additionalArgumentsAsList ?: emptyList())
    return allArgs
        .filterDuplicatedCompilerPluginOptions()
        .filterDuplicatedWarningLevel()
}

/*
* This function filters duplicates of -P plugin:<pluginId>:<optionName>=<value> in the compiler arguments
*/
internal fun List<String>.filterDuplicatedCompilerPluginOptions(): List<String> {
    val filteredArguments = mutableListOf<String>()
    val knownPluginOptions = mutableSetOf<String>()
    val argumentsIterator = this.iterator()

    while (argumentsIterator.hasNext()) {
        val argument = argumentsIterator.next()
        // try to find pair -P plugin:<pluginId>:<optionName>=<value>
        if (argument == "-P" && argumentsIterator.hasNext()) {
            val pluginOption = argumentsIterator.next() // expected plugin:<pluginId>:<optionName>=<value>
            val elementIsUnique = knownPluginOptions.add(pluginOption)
            if (elementIsUnique) {
                filteredArguments.add(argument) // add -P
                filteredArguments.add(pluginOption) // add the plugin option
            }
        } else {
            // skip filtering for all other arguments
            filteredArguments.add(argument)
        }
    }

    return filteredArguments
}

/**
 * Removes duplicate `-Xwarning-level` arguments from a compiler argument list,
 * keeping only the first occurrence of each unique `-Xwarning-level=<diagnostic>:<level>` entry
 */
internal fun List<String>.filterDuplicatedWarningLevel(): List<String> {
    val warningLevelArgumentsAccumulator = mutableSetOf<String>()
    return filter { !it.startsWith("-Xwarning-level") || warningLevelArgumentsAccumulator.add(it) }
}
