/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.core

import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.mppSources
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleSubType
import org.junit.Test
import kotlin.test.assertEquals

class ExpectedFileTest {
    @Test
    fun `it should generate valid expect functions`() {
        val file = mppSources {
            mppFile("file") {
                function("f(x: Int): String")
                function("y(x: Int): String")
            }
        }.mppFiles.first()
        val generatedFileText = file.printForModuleSubType(ModuleSubType.common)
        val expectedFileText = """
            expect fun f(x: Int): String
            
            expect fun y(x: Int): String            
        """.trimIndent().trim()
        assertEquals(expectedFileText, generatedFileText)
    }

    @Test
    fun `it should generate valid expect classes`() {
        val file = mppSources {
            mppFile("file") {
                `class`("A")
                `class`("B")
            }
        }.mppFiles.first()
        val generatedFileText = file.printForModuleSubType(ModuleSubType.common)
        val expectedFileText = """
           expect class A()

           expect class B()
        """.trimIndent().trim()
        assertEquals(expectedFileText, generatedFileText)
    }

    @Test
    fun `it should generate valid actual functions`() {
        val file = mppSources {
            mppFile("file") {
                function("foo()") {
                    actualFor(
                        ModuleSubType.jvm,
                        actualBody = "println(\"jvm\")\nval a = 10"
                    )
                    actualFor(ModuleSubType.js, actualBody = """println("js")""")
                    default("TODO()")
                }
            }
        }.mppFiles.first()
        run {
            val generatedFileText = file.printForModuleSubType(ModuleSubType.jvm)
            val expectedFileText = """
            actual fun foo() {
                println("jvm")
                val a = 10
            }
           """.trimIndent().trim()
            assertEquals(expectedFileText, generatedFileText)
        }

        run {
            val generatedFileText = file.printForModuleSubType(ModuleSubType.js)
            val expectedFileText = """
            actual fun foo() {
                println("js")
            }
           """.trimIndent().trim()
            assertEquals(expectedFileText, generatedFileText)
        }
        run {
            val generatedFileText = file.printForModuleSubType(ModuleSubType.android)
            val expectedFileText = """
            actual fun foo() {
                TODO()
            }
           """.trimIndent().trim()
            assertEquals(expectedFileText, generatedFileText)
        }
    }

    @Test
    fun `it should generate valid actual classes`() {
        val file = mppSources {
            mppFile("file") {
                `class`("A") {
                    actualFor(
                        ModuleSubType.jvm,
                        actualBody = "val a = 10\n fun x(): String"
                    )
                    actualFor(ModuleSubType.js, actualBody = "val b = 10\n fun y(): String")
                    default("// nothing here")
                }
            }
        }.mppFiles.first()
        run {
            val generatedFileText = file.printForModuleSubType(ModuleSubType.jvm)
            val expectedFileText = """
            actual class A actual constructor() {
                val a = 10
                fun x(): String
            }
           """.trimIndent().trim()
            assertEquals(expectedFileText, generatedFileText)
        }

        run {
            val generatedFileText = file.printForModuleSubType(ModuleSubType.js)
            val expectedFileText = """
             actual class A actual constructor() {
                val b = 10
                fun y(): String
            }
           """.trimIndent().trim()
            assertEquals(expectedFileText, generatedFileText)
        }
        run {
            val generatedFileText = file.printForModuleSubType(ModuleSubType.android)
            val expectedFileText = """
            actual class A actual constructor() {
                // nothing here
            }
           """.trimIndent().trim()
            assertEquals(expectedFileText, generatedFileText)
        }
    }
}