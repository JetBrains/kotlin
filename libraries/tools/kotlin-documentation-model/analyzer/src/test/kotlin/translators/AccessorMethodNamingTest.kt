/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package translators

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.DProperty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * https://kotlinlang.org/docs/java-to-kotlin-interop.html#properties
 * https://kotlinlang.org/docs/java-interop.html#getters-and-setters
 */
class AccessorMethodNamingTest : BaseAbstractTest() {

    @Test
    fun `standard property`() {
        testAccessors("data class TestCase(var standardString: String, var standardBoolean: Boolean)") {
            doTest("standardString", "getStandardString", "setStandardString")
            doTest("standardBoolean", "getStandardBoolean", "setStandardBoolean")
        }
    }

    @Test
    fun `properties that start with the word 'is' use the special is rules`() {
        testAccessors("data class TestCase(var isFoo: String, var isBar: Boolean)") {
            doTest("isFoo", "isFoo", "setFoo")
            doTest("isBar", "isBar", "setBar")
        }
    }

    @Test
    fun `properties that start with a word that starts with 'is' use get and set`() {
        testAccessors("data class TestCase(var issuesFetched: Int, var issuesWereDisplayed: Boolean)") {
            doTest("issuesFetched", "getIssuesFetched", "setIssuesFetched")
            doTest("issuesWereDisplayed", "getIssuesWereDisplayed", "setIssuesWereDisplayed")
        }
    }

    @Test
    fun `properties that start with the word 'is' followed by underscore use the special is rules`() {
        testAccessors("data class TestCase(var is_foo: String, var is_bar: Boolean)") {
            doTest("is_foo", "is_foo", "set_foo")
            doTest("is_bar", "is_bar", "set_bar")
        }
    }

    @Test
    fun `properties that start with the word 'is' followed by a number use the special is rules`() {
        testAccessors("data class TestCase(var is1of: String, var is2of: Boolean)") {
            doTest("is1of", "is1of", "set1of")
            doTest("is2of", "is2of", "set2of")
        }
    }

    @Test
    fun `sanity check short names`() {
        testAccessors(
            """
            data class TestCase(
               var i: Boolean,
               var `is`: Boolean,
               var isz: Boolean,
               var isA: Int,
               var isB: Boolean,
            )
            """.trimIndent()
        ) {
            doTest("i", "getI", "setI")
            doTest("is", "getIs", "setIs")
            doTest("isz", "getIsz", "setIsz")
            doTest("isA", "isA", "setA")
            doTest("isB", "isB", "setB")
        }
    }

    private fun testAccessors(code: String, block: PropertyTestCase.() -> Unit) {
        val configuration = dokkaConfiguration {
            suppressObviousFunctions = false
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin")
                }
            }
        }

        testInline("""
            /src/main/kotlin/sample/TestCase.kt
            package sample
            
            $code
            """.trimIndent(),
            configuration) {
            documentablesMergingStage = { module ->
                val properties = module.packages.single().classlikes.first().properties
                PropertyTestCase(properties).apply {
                    block()
                    finish()
                }
            }
        }
    }

    private class PropertyTestCase(private val properties: List<DProperty>) {
        private var testsDone: Int = 0

        fun doTest(kotlinName: String, getter: String? = null, setter: String? = null) {
            properties.first { it.name == kotlinName }.let {
                assertEquals(getter, it.getter?.name)
                assertEquals(setter, it.setter?.name)
            }
            testsDone += 1
        }

        fun finish() {
            assertTrue(testsDone > 0, "No tests in TestCase")
            assertEquals(testsDone, properties.size)
        }
    }
}
