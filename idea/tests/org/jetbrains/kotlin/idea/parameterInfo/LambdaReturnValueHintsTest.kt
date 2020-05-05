/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.parameterInfo

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LineExtensionInfo
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.testFramework.utils.inlays.InlayHintsChecker
import org.jetbrains.kotlin.idea.codeInsight.hints.HintType
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.jetbrains.kotlin.test.TagsTestDataUtil
import org.junit.Assert
import org.junit.runner.RunWith

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class LambdaReturnValueHintsTest : KotlinLightCodeInsightFixtureTestCase() {
    companion object {
        const val DISABLE_ACTION_TEXT = "Do not show lambda return expression hints"
        const val ENABLE_ACTION_TEXT = "Show lambda return expression hints"
    }

    override fun getProjectDescriptor(): KotlinLightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    private class LineExtensionInfoTag(offset: Int, data: LineExtensionInfo) :
        TagsTestDataUtil.TagInfo<LineExtensionInfo>(offset, true, true, false, data) {

        override fun getName() = "hint"
        override fun getAttributesString(): String = "text=\"${data.text}\""
    }

    private class CaretTag(editor: Editor) :
        TagsTestDataUtil.TagInfo<Any>(
            editor.caretModel.currentCaret.offset,
            /*isStart = */true, /*isClosed = */false, /*isFixed = */true,
            "caret"
        )


    private fun collectActualLineExtensionsTags(): List<LineExtensionInfoTag> {
        val tags = ArrayList<LineExtensionInfoTag>()
        val lineCount = myFixture.editor.document.lineCount
        for (i in 0 until lineCount) {
            val lineEndOffset = myFixture.editor.document.getLineEndOffset(i)

            (myFixture.editor as EditorImpl).processLineExtensions(i) { lineExtensionInfo ->
                tags.add(LineExtensionInfoTag(lineEndOffset, lineExtensionInfo))
                true
            }
        }

        return tags
    }

    fun check(text: String) {
        myFixture.configureByText("A.kt", text.trimIndent())

        val expectedText = run {
            val tags = if (editor.caretModel.offset > 0) listOf(CaretTag(editor)) else emptyList()
            TagsTestDataUtil.insertTagsInText(tags, editor.document.text) { null }
        }

        // Clean test file from the hints tags
        InlayHintsChecker(myFixture).extractInlaysAndCaretInfo(editor.document)

        myFixture.doHighlighting()

        Assert.assertTrue(
            "No other inlays should be present in the file",
            editor.inlayModel.getInlineElementsInRange(0, editor.document.textLength).isEmpty()
        )

        if (editor.caretModel.offset > 0) {
            val availableIntentions = myFixture.availableIntentions
            Assert.assertTrue(
                "Disable action with text `$DISABLE_ACTION_TEXT` is expected: \n${availableIntentions.joinToString(separator = "\n") { "  $it" }}",
                availableIntentions.any { it.text == DISABLE_ACTION_TEXT }
            )
        }

        val actualText = run {
            val tags = ArrayList<TagsTestDataUtil.TagInfo<*>>()

            if (editor.caretModel.offset > 0) {
                tags.add(CaretTag(editor))
            }

            tags.addAll(collectActualLineExtensionsTags())

            TagsTestDataUtil.insertTagsInText(tags, editor.document.text) { null }
        }

        Assert.assertEquals(expectedText, actualText)
    }

    fun testDisableEnableActions() {
        myFixture.configureByText(
            "A.kt",
            """
            val x = run {
                println("foo")
                1<caret>
            }
            """.trimIndent()
        )

        Assert.assertTrue("Return expression hint should be enabled", HintType.LAMBDA_RETURN_EXPRESSION.enabled)
        try {
            val disableIntention = findDisableReturnHintsIntention()
            disableIntention.invoke(project, editor, file)

            Assert.assertFalse("Disable action doesn't work", HintType.LAMBDA_RETURN_EXPRESSION.option.get())

            val availableIntentions = myFixture.availableIntentions
            Assert.assertTrue(
                intentionsPresenceErrorMessage(
                    "Disable action shouldn't be present when option is already disabled", availableIntentions
                ),
                availableIntentions.find { it.text == DISABLE_ACTION_TEXT } == null
            )

            val enableAction = availableIntentions.find { it.text == ENABLE_ACTION_TEXT }
            Assert.assertTrue(
                intentionsPresenceErrorMessage("No enable action with text $ENABLE_ACTION_TEXT found", availableIntentions),
                enableAction != null
            )

            enableAction!!.invoke(project, editor, file)
            Assert.assertTrue("Enable action doesn't work", HintType.LAMBDA_RETURN_EXPRESSION.option.get())

        } finally {
            HintType.LAMBDA_RETURN_EXPRESSION.option.set(true)
        }

    }

    private fun findDisableReturnHintsIntention(): IntentionAction {
        val availableIntentions = myFixture.availableIntentions
        return availableIntentions.find { it.text == DISABLE_ACTION_TEXT }
            ?: throw AssertionError(
                intentionsPresenceErrorMessage("Disable action with text `$DISABLE_ACTION_TEXT` is expected", availableIntentions)
            )
    }

    private fun intentionsPresenceErrorMessage(message: String, intentions: List<IntentionAction>): String {
        return "$message: \n" +
                intentions.joinToString(separator = "\n") { "  $it" }
    }

    fun testSimple() {
        check(
            """
            val x = run {
                println("foo")
                1<caret><hint text=" "/><hint text="^run"/>
            }
            """
        )
    }

    fun testQualified() {
        check(
            """
            val x = run {
                var s = "abc"
                s.length<caret><hint text=" "/><hint text="^run"/>
            }
            """
        )
    }

    fun testIf() {
        check(
            """
            val x = run {
                if (true) {
                    1<hint text=" "/><hint text="^run"/>
                } else {
                    0<hint text=" "/><hint text="^run"/>
                }
            }
            """
        )
    }

    fun testOneLineIf() {
        check(
            """
            val x = run {
                println(1)
                if (true) 1 else { 0 }<caret><hint text=" "/><hint text="^run"/>
            }
            """
        )
    }

    fun testWhen() {
        check(
            """
            val x = run {
                when (true) {
                    true -> 1<hint text=" "/><hint text="^run"/>
                    false -> 0<hint text=" "/><hint text="^run"/>
                }
            }
            """
        )
    }

    fun testNoHintForSingleExpression() {
        check(
            """
            val x = run {
                1
            }
            """
        )
    }

    fun testLabel() {
        check(
            """
            val x = run foo@{
                println("foo")
                1<hint text=" "/><hint text="^foo"/>
            }
            """
        )
    }

    fun testNested() {
        check(
            """
            val x = run hello@{
                if (true) {
                }

                run { // Two hints here
                    when (true) {
                        true -> 1<hint text=" "/><hint text="^run"/>
                        false -> 0<hint text=" "/><hint text="^run"/>
                    }
                }<hint text=" "/><hint text="^hello"/>
            }
            """
        )
    }

    fun testElvisOperator() {
        check(
            """
            fun foo() {
                run {
                    val length: Int? = null
                    length ?: 0<hint text=" "/><hint text="^run"/>
                }
            }
            """
        )
    }

    fun testPostfixPrefixExpressions() {
        check(
            """
            fun bar() {
                var test = 0
                run {
                    test
                    test++<hint text=" "/><hint text="^run"/>
                }

                run {
                    test
                    ++test<hint text=" "/><hint text="^run"/>
                }
            }
            """
        )
    }

    fun testAnnotatedStatement() {
        check(
            """
            @Target(AnnotationTarget.EXPRESSION)
            annotation class Some

            fun test() {
                run {
                    val files: Any? = null
                    @Some
                    12<hint text=" "/><hint text="^run"/>
                }

                run {
                    val files: Any? = null
                    @Some 12<hint text=" "/><hint text="^run"/>
                }
            }
            """
        )
    }

    fun testLabeledStatement() {
        check(
            """
            fun test() {
                run {
                    val files: Any? = null
                    run@
                    12<hint text=" "/><hint text="^run"/>
                }

                run {
                    val files: Any? = null
                    run@12<hint text=" "/><hint text="^run"/>
                }
            }
            """
        )
    }

    fun testReturnFunctionType() {
        check(
            """
            fun test() = run {
                val a = 1
                { a }<hint text=" "/><hint text="^run"/>
            }
            """
        )
    }
}
