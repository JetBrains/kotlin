/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test

@Suppress("EnumEntryName")
internal enum class IgnoredTestSuitesReporting {
    skip, reportAsIgnoredTest, reportAllInnerTestsAsIgnored
}

internal class FrameworkTestArguments(
    val includedQualifiers: List<String>,
    val includedClassMethods: List<Pair<String, String>>,
    val excludedQualifiers: List<String>,
    val excludedClassMethods: List<Pair<String, String>>,
    val ignoredTestSuites: IgnoredTestSuitesReporting,
    val dryRun: Boolean
) {
    companion object {
        fun parse(args: List<String>): FrameworkTestArguments {
            var isInclude = false
            var isExclude = false

            val includesClassMethods = mutableListOf<Pair<String, String>>()
            val includesQualifiers = mutableListOf<String>()
            val excludesClassMethods = mutableListOf<Pair<String, String>>()
            val excludesQualifiers = mutableListOf<String>()

            fun addToIncludeOrExcludeList(argument: String) {
                if (argument.isEmpty()) return
                if (argument[0].let { it != it.lowercaseChar() }){
                    val dotIndex = argument.indexOf('.')
                    val listToAdd = if (isInclude) includesClassMethods else excludesClassMethods
                    if (dotIndex == -1) {
                        listToAdd.add(argument to "*")
                    } else {
                        if (dotIndex < 1 || dotIndex >= argument.lastIndex) return
                        val className = argument.substring(0, dotIndex)
                        val methodName = argument.substring(dotIndex + 1)
                        listToAdd.add(className to methodName)
                    }
                } else {
                    (if (isInclude) includesQualifiers else excludesQualifiers).add(argument)
                }
            }

            var ignoredTestSuites: IgnoredTestSuitesReporting = IgnoredTestSuitesReporting.reportAllInnerTestsAsIgnored
            var isIgnoredTestSuites = false
            var dryRun = false

            for (arg in args) {
                if (isInclude || isExclude) {
                    for (splitArg in arg.split(',')) {
                        addToIncludeOrExcludeList(splitArg)
                    }
                    isInclude = false
                    isExclude = false
                    continue
                }

                if (isIgnoredTestSuites) {
                    val value = IgnoredTestSuitesReporting.values().firstOrNull { it.name == arg }
                    if (value != null) {
                        ignoredTestSuites = value
                    }
                    isIgnoredTestSuites = false
                    continue
                }

                when (arg) {
                    "--include" -> isInclude = true
                    "--exclude" -> isExclude = true
                    "--ignoredTestSuites" -> isIgnoredTestSuites = true
                    "--dryRun" -> dryRun = true
                }
            }

            if (includesClassMethods.isEmpty() && includesQualifiers.isEmpty()) {
                includesQualifiers.add("*")
            }

            return FrameworkTestArguments(
                includedQualifiers = includesQualifiers,
                includedClassMethods = includesClassMethods,
                excludedQualifiers = excludesQualifiers,
                excludedClassMethods = excludesClassMethods,
                ignoredTestSuites = ignoredTestSuites,
                dryRun = dryRun
            )
        }
    }
}