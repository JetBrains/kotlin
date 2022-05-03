/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test

import kotlin.test.FrameworkAdapter
import kotlin.math.abs

@JsFun("() => (typeof arguments !== 'undefined' && typeof arguments.join !== 'undefined') ? arguments.join(' ') : '' ")
private external fun d8Arguments(): String
@JsFun("() => (typeof process != 'undefined' && typeof process.argv != 'undefined') ? process.argv.slice(2).join(' ') : ''")
private external fun nodeArguments(): String

internal class TeamcityAdapter : FrameworkAdapter {
    private enum class MessageType(val type: String) {
        Started("testStarted"),
        Finished("testFinished"),
        Failed("testFailed"),
        Ignored("testIgnored"),
        SuiteStarted("testSuiteStarted"),
        SuiteFinished("testSuiteFinished"),
    }

    private fun String?.tcEscape(): String = this
        ?.substring(0, length.coerceAtMost(7000))
        ?.replace("|", "||")
        ?.replace("\'", "|'")
        ?.replace("\n", "|n")
        ?.replace("\r", "|r")
        ?.replace("\u0085", "|x") // next line
        ?.replace("\u2028", "|l") // line separator
        ?.replace("\u2029", "|p") // paragraph separator
        ?.replace("[", "|[")
        ?.replace("]", "|]")
        ?: ""

    private val flowId: String = "flowId='wasmTcAdapter${abs(hashCode())}'"

    private fun MessageType.report(name: String) {
        println("##teamcity[$type name='${name.tcEscape()}' $flowId]")
    }

    private fun MessageType.report(name: String, e: Throwable) {
        if (this == MessageType.Failed) {
            println("##teamcity[$type name='${name.tcEscape()}' message='${e.message.tcEscape()}' details='${e.stackTraceToString().tcEscape()}' $flowId]")
        } else {
            println("##teamcity[$type name='${name.tcEscape()}' text='${e.message.tcEscape()}' errorDetails='${e.stackTraceToString().tcEscape()}' status='ERROR' $flowId]")
        }
    }

    private val testArguments: FrameworkTestArguments by lazy {
        val arguments = d8Arguments().takeIf { it.isNotEmpty() } ?: nodeArguments()
        FrameworkTestArguments.parse(arguments.split(' '))
    }

    private fun runSuite(name: String, suiteFn: () -> Unit) {
        MessageType.SuiteStarted.report(name)
        try {
            suiteFn()
            MessageType.SuiteFinished.report(name)
        } catch (e: Throwable) {
            MessageType.SuiteFinished.report(name, e)
        }
    }

    private fun runIgnoredSuite(name: String, suiteFn: () -> Unit) {
        MessageType.SuiteStarted.report(name)
        suiteFn()
        MessageType.SuiteFinished.report(name)
    }

    private var isUnderIgnoredSuit: Boolean = false
    private var qualifiedName: String = ""
    private var className: String = ""
    private inline fun enterIfIncluded(name: String, isSuit: Boolean, body: () -> Unit) {
        if (name.isEmpty()) {
            body()
            return
        }

        val oldQualifiedName = qualifiedName
        val oldClassName = className
        try {
            qualifiedName = if (oldQualifiedName.isEmpty()) name else "$oldQualifiedName.$name"
            className = name
            if (isSuit) {
                body()
            } else {
                val runTest = testArguments.run {
                    val included =
                        includedClassMethods.any { it.first == oldClassName && name.matched(it.second) } ||
                                includedQualifiers.any { oldQualifiedName.matched(it) || qualifiedName.matched(it) }
                    included &&
                            excludedClassMethods.none { it.first == oldClassName && name.matched(it.second) } &&
                            excludedQualifiers.none { oldQualifiedName.matched(it) || qualifiedName.matched(it) }
                }
                if (runTest) {
                    body()
                }
            }
        } finally {
            qualifiedName = oldQualifiedName
            className = oldClassName
        }
    }

    override fun suite(name: String, ignored: Boolean, suiteFn: () -> Unit) {
        if (isUnderIgnoredSuit) {
            runIgnoredSuite(name, suiteFn)
            return
        }

        if (!ignored) {
            enterIfIncluded(name, true) {
                runSuite(name, suiteFn)
            }
        } else {
            when (testArguments.ignoredTestSuites) {
                IgnoredTestSuitesReporting.reportAsIgnoredTest -> {
                    MessageType.Ignored.report(name)
                }
                IgnoredTestSuitesReporting.reportAllInnerTestsAsIgnored -> {
                    var oldIsUnderIgnoredSuit = isUnderIgnoredSuit
                    isUnderIgnoredSuit = true
                    try {
                        runIgnoredSuite(name, suiteFn)
                    } finally {
                        isUnderIgnoredSuit = oldIsUnderIgnoredSuit
                    }
                }
                IgnoredTestSuitesReporting.skip -> { }
            }
        }
    }

    override fun test(name: String, ignored: Boolean, testFn: () -> Any?) {
        if (isUnderIgnoredSuit) {
            MessageType.Ignored.report(name)
            return
        }

        if (ignored) {
            MessageType.Ignored.report(name)
            return
        }

        enterIfIncluded(name, false) {
            try {
                MessageType.Started.report(name)
                if (!testArguments.dryRun) {
                    testFn()
                }
            } catch (e: Throwable) {
                MessageType.Failed.report(name, e)
            }
            MessageType.Finished.report(name)
        }
    }
}

private fun String.matched(prefix: String, startSourceIndex: Int = 0, startPrefixIndex: Int = 0): Boolean {
    var sourceIndex = startSourceIndex
    var prefixIndex = startPrefixIndex

    if (prefixIndex == prefix.lastIndex && prefix[prefixIndex] == '*') return true
    if (sourceIndex > this.lastIndex) return false

    while (prefixIndex <= prefix.lastIndex && sourceIndex <= this.lastIndex) {
        val currentSearchSymbol = prefix[prefixIndex]
        if (currentSearchSymbol == '*') {
            if (prefixIndex == prefix.lastIndex) return true
            val searchSymbolAfterStar = prefix[prefixIndex + 1]
            val foundIndexOfSymbolAfterStar = this.indexOf(searchSymbolAfterStar, sourceIndex)
            if (foundIndexOfSymbolAfterStar == -1) return false
            if (this.matched(prefix, foundIndexOfSymbolAfterStar + 1, prefixIndex)) return true
            sourceIndex = foundIndexOfSymbolAfterStar
        } else {
            if (currentSearchSymbol != this[sourceIndex]) return false
            sourceIndex++
        }
        prefixIndex++
    }

    return sourceIndex > this.lastIndex &&
            (prefixIndex > prefix.lastIndex || (prefixIndex == prefix.lastIndex && prefix[prefixIndex] == '*'))
}