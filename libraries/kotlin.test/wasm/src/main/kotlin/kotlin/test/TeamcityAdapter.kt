/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test

import kotlin.test.FrameworkAdapter

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

    private fun MessageType.report(name: String) {
        println("##teamcity[$type name='${name.tcEscape()}']")
    }

    private fun MessageType.report(name: String, e: Throwable) {
        if (this == MessageType.Failed) {
            println("##teamcity[$type name='${name.tcEscape()}' message='${e.message.tcEscape()}' details='${e.stackTraceToString().tcEscape()}']")
        } else {
            println("##teamcity[$type name='${name.tcEscape()}' text='${e.message.tcEscape()}' errorDetails='${e.stackTraceToString().tcEscape()}' status='ERROR']")
        }
    }

    override fun suite(name: String, ignored: Boolean, suiteFn: () -> Unit) {
        MessageType.SuiteStarted.report(name)
        if (!ignored) {
            try {
                suiteFn()
                MessageType.SuiteFinished.report(name)
            } catch (e: Throwable) {
                MessageType.SuiteFinished.report(name, e)
            }
        }
    }

    override fun test(name: String, ignored: Boolean, testFn: () -> Any?) {
        if (ignored) {
            MessageType.Ignored.report(name)
        } else {
            try {
                MessageType.Started.report(name)
                testFn()
            } catch (e: Throwable) {
                MessageType.Failed.report(name, e)
            }
            MessageType.Finished.report(name)
        }
    }
}