package org.jetbrains.kotlin.idea.refactoring.inline

import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.psi.KtFile

class FindReferenceInEditorTest : KotlinLightCodeInsightFixtureTestCase() {
    fun `test function, caret at start`() = doTest(
        """
            fun test() {}
            fun println(any: Any) {}
            fun usage() {
                println(<caret>test())
            }
        """.trimIndent()
    )

    fun `test function, caret at end`() = doTest(
        """
            fun test() {}
            fun println(any: Any) {}
            fun usage() {
                println(test<caret>())
            }
        """.trimIndent()
    )

    fun `test function, caret at middle`() = doTest(
        """
            fun test() {}
            fun println(any: Any) {}
            fun usage() {
                println(te<caret>st())
            }
        """.trimIndent()
    )

    fun `test short function, caret at start`() = doTest(
        """
            fun t() {}
            fun println(any: Any) {}
            fun usage() {
                println(<caret>t())
            }
        """.trimIndent()
    )

    fun `test short function, caret at end`() = doTest(
        """
            fun t() {}
            fun println(any: Any) {}
            fun usage() {
                println(t<caret>())
            }
        """.trimIndent()
    )

    fun `test println function`() = doTest(
        """
            fun println(any: Any) {}
            fun test() {}
            fun usage() {
                println<caret>(test())
            }
        """.trimIndent()
    )

    fun `test property, caret at start`() = doTest(
        """
            val name = "name"
            fun println(any: Any) {}
            fun usage() {
                println(<caret>name)
            }
        """.trimIndent()
    )

    fun `test property, caret at end`() = doTest(
        """
            val name = "name"
            fun println(any: Any) {}
            fun usage() {
                println(name<caret>)
            }
        """.trimIndent()
    )

    fun `test property, caret at middle`() = doTest(
        """
            val name = "name"
            fun println(any: Any) {}
            fun usage() {
                println(na<caret>me)
            }
        """.trimIndent()
    )

    fun `test short property, caret at start`() = doTest(
        """
            val v = "name"
            fun println(any: Any) {}
            fun usage() {
                println(<caret>v)
            }
        """.trimIndent()
    )

    fun `test short property, caret at end`() = doTest(
        """
            val v = "name"
            fun println(any: Any) {}
            fun usage() {
                println(v<caret>)
            }
        """.trimIndent()
    )

    private fun doTest(text: String) {
        val file = myFixture.configureByText("dummy.kt", text) as KtFile
        val reference = myFixture.editor.findSimpleNameReference()
        assertInstanceOf(reference, KtSimpleNameReference::class.java)
        assertEquals(file.declarations.first(), reference?.resolve())
    }
}