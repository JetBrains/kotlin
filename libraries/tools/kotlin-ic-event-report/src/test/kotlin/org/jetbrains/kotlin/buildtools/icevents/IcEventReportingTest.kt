/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.icevents

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.buildtools.api.trackers.FileCompilationReason
import org.jetbrains.kotlin.buildtools.api.trackers.IcEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class IcEventReportingTest {
    @Test
    fun goodEvents() {
        val event1 = object : IcEvent.CompilationStarted {
            override val isIncremental = true
            override val reason = null
            override val type = "CompilationStarted"
            override val timestamp = 0L
            override val iteration = 0
        }
        val event2 = object : IcEvent.CompileIteration {
            override val reasons = mapOf(
                "KotlinFile.kt" to listOf(object : FileCompilationReason.Basic {
                    override val readableString = "Wanted to compile"
                })
            )
            override val exitCode = "OK"
            override val type = "CompileIteration"
            override val timestamp = 0L
            override val iteration = 0
        }
        decodeIcEvents(encodeIcEvents(listOf(event1, event2))).let {
            assertEquals(2, it.size)
            val result1 = it.first()
            assertIs<IcEvent.CompilationStarted>(result1)
            assertEquals(event1.isIncremental, result1.isIncremental)
            assertEquals(event1.reason, result1.reason)
            assertEquals(event1.timestamp, result1.timestamp)
            assertEquals(event1.iteration, result1.iteration)
            val result2 = it.last()
            assertIs<IcEvent.CompileIteration>(result2)
            assertEquals(
                event2.reasons["KotlinFile.kt"]!!.single().readableString,
                result2.reasons["KotlinFile.kt"]!!.single().readableString
            )
            assertEquals("OK", result2.exitCode)
        }
    }

    @Test
    fun newField() {
        @Language("JSON") val event = """
            {
                "events": [
                    {
                        "isIncremental": true,
                        "reason": null,
                        "timestamp": 1676767676767,
                        "iteration": 0,
                        "type": "CompilationStarted",
                        "newField": "New field, yo"
                    }
                ]
            }
        """.trimIndent()
        decodeIcEvents(event).let {
            val result = it.single()
            assertIs<IcEvent.CompilationStarted>(result)
            assertEquals(true, result.isIncremental)
            assertEquals(null, result.reason)
            assertEquals(1676767676767, result.timestamp)
            assertEquals(0, result.iteration)
        }
    }

    @Test
    fun newType() {
        @Language("JSON") val event = """
            {
                "events": [
                    {
                        "isIncremental": true,
                        "reason": null,
                        "timestamp": 1676767676767,
                        "iteration": 0,
                        "type": "NewType",
                        "newField": "New field, yo"
                    }
                ]
            }
        """.trimIndent()
        val result = decodeIcEvents(event)
        assertEquals(emptyList(), result)
    }

    @Test
    fun newReason() {
        @Language("JSON") val event = """
            {
                "events": [
                    {
                        "reasons": {
                            "KotlinFile.kt": [
                                {
                                    "readableString": "Reasons, reasons",
                                    "newField": "New field, yo",
                                    "anotherNewField": "Another new field, yo"
                                }
                            ]
                        },
                        "exitCode": "OK",
                        "timestamp": 1676767676767,
                        "iteration": 0,
                        "type": "CompileIteration"
                    }
                ]
            }
        """.trimIndent()
        decodeIcEvents(event).let {
            val result = it.single()
            assertIs<IcEvent.CompileIteration>(result)
            assertEquals("Reasons, reasons", result.reasons["KotlinFile.kt"]!!.single().readableString)
            assertEquals("OK", result.exitCode)
            assertEquals(1676767676767, result.timestamp)
            assertEquals(0, result.iteration)
        }
    }
}
