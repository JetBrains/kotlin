/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.editor

import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.LightCodeInsightTestCase
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import com.intellij.testFramework.LightPlatformTestCase
import org.jetbrains.kotlin.idea.KotlinFileType

class TypedHandlerTest : LightCodeInsightTestCase() {
    private val dollar = '$'

    fun testTypeStringTemplateStart() = doCharTypeTest(
            '{',
            """val x = "$<caret>" """,
            """val x = "$dollar{}" """
    )

    fun testAutoIndentRightOpenBrace() = doCharTypeTest(
            '{',

            "fun test() {\n" +
            "<caret>\n" +
            "}",

            "fun test() {\n" +
            "    {<caret>}\n" +
            "}"
    )

    fun testAutoIndentLeftOpenBrace() = doCharTypeTest(
            '{',

            "fun test() {\n" +
            "      <caret>\n" +
            "}",

            "fun test() {\n" +
            "    {<caret>}\n" +
            "}"
    )

    fun testTypeStringTemplateStartWithCloseBraceAfter() = doCharTypeTest(
            '{',
            """fun foo() { "$<caret>" }""",
            """fun foo() { "$dollar{}" }"""
    )

    fun testTypeStringTemplateStartBeforeString() = doCharTypeTest(
            '{',
            """fun foo() { "$<caret>something" }""",
            """fun foo() { "$dollar{}something" }"""
    )

    fun testKT3575() = doCharTypeTest(
            '{',
            """val x = "$<caret>]" """,
            """val x = "$dollar{}]" """
    )

    fun testAutoCloseRawStringInEnd() = doCharTypeTest(
            '"',
            """val x = ""<caret>""",
            """val x = ""${'"'}<caret>""${'"'}"""
    )

    fun testNoAutoCloseRawStringInEnd() = doCharTypeTest(
            '"',
            """val x = ""${'"'}<caret>""",
            """val x = ""${'"'}""""
    )

    fun testAutoCloseRawStringInMiddle() = doCharTypeTest(
            '"',
            """
            val x = ""<caret>
            val y = 12
            """.trimIndent(),
            """
            val x = ""${'"'}<caret>""${'"'}
            val y = 12
            """.trimIndent()
    )

    fun testNoAutoCloseBetweenMultiQuotes() = doCharTypeTest(
            '"',
            """val x = ""${'"'}<caret>${'"'}""/**/""",
            """val x = ""${'"'}${'"'}<caret>""/**/"""
    )

    fun testNoAutoCloseBetweenMultiQuotes1() = doCharTypeTest(
            '"',
            """val x = ""${'"'}"<caret>"${'"'}/**/""",
            """val x = ""${'"'}""<caret>${'"'}/**/"""
    )

    fun testNoAutoCloseAfterEscape() = doCharTypeTest(
        '"',
        """val x = "\""<caret>""",
        """val x = "\""${'"'}<caret>""""
    )

    fun testAutoCloseBraceInFunctionDeclaration() = doCharTypeTest(
            '{',
            "fun foo() <caret>",
            "fun foo() {<caret>}"
    )

    fun testAutoCloseBraceInLocalFunctionDeclaration() = doCharTypeTest(
            '{',

            "fun foo() {\n" +
            "    fun bar() <caret>\n" +
            "}",

            "fun foo() {\n" +
            "    fun bar() {<caret>}\n" +
            "}"
    )

    fun testAutoCloseBraceInAssignment() = doCharTypeTest(
            '{',
            "fun foo() {\n" +
            "    val a = <caret>\n" +
            "}",

            "fun foo() {\n" +
            "    val a = {<caret>}\n" +
            "}"
    )

    fun testDoNotAutoCloseBraceInUnfinishedIfSurroundOnSameLine() = doCharTypeTest(
            '{',

            "fun foo() {\n" +
            "    if() <caret>foo()\n" +
            "}",

            "fun foo() {\n" +
            "    if() {foo()\n" +
            "}"
    )

    fun testDoNotAutoCloseBraceInUnfinishedElseSurroundOnSameLine() = doCharTypeTest(
            '{',

            "fun foo() {\n" +
            "    if(true) {} else <caret>foo()\n" +
            "}",

            "fun foo() {\n" +
            "    if(true) {} else {foo()\n" +
            "}"
    )

    fun testDoNotAutoCloseBraceInUnfinishedTryOnSameLine() = doCharTypeTest(
            '{',

            "fun foo() {\n" +
            "    try <caret>foo()\n" +
            "}",

            "fun foo() {\n" +
            "    try {foo()\n" +
            "}"
    )

    fun testDoNotAutoCloseBraceInUnfinishedCatchOnSameLine() = doCharTypeTest(
            '{',

            "fun foo() {\n" +
            "    try {} catch (e: Exception) <caret>foo()\n" +
            "}",

            "fun foo() {\n" +
            "    try {} catch (e: Exception) {foo()\n" +
            "}"
    )

    fun testDoNotAutoCloseBraceInUnfinishedFinallyOnSameLine() = doCharTypeTest(
            '{',

            "fun foo() {\n" +
            "    try {} catch (e: Exception) finally <caret>foo()\n" +
            "}",

            "fun foo() {\n" +
            "    try {} catch (e: Exception) finally {foo()\n" +
            "}"
    )

    fun testDoNotAutoCloseBraceInUnfinishedWhileSurroundOnSameLine() = doCharTypeTest(
            '{',

            "fun foo() {\n" +
            "    while() <caret>foo()\n" +
            "}",

            "fun foo() {\n" +
            "    while() {foo()\n" +
            "}"
    )

    fun testDoNotAutoCloseBraceInUnfinishedWhileSurroundOnNewLine() = doCharTypeTest(
            '{',

            "fun foo() {\n" +
            "    while()\n" +
            "<caret>\n" +
            "    foo()\n" +
            "}",

            "fun foo() {\n" +
            "    while()\n" +
            "    {\n" +
            "    foo()\n" +
            "}"
    )

    fun testDoNotAutoCloseBraceInUnfinishedIfSurroundOnOtherLine() = doCharTypeTest(
            '{',

            "fun foo() {\n" +
            "    if(true) <caret>\n" +
            "    foo()\n" +
            "}",

            "fun foo() {\n" +
            "    if(true) {<caret>\n" +
            "    foo()\n" +
            "}"
    )

    fun testDoNotAutoCloseBraceInUnfinishedElseSurroundOnOtherLine() = doCharTypeTest(
            '{',

            "fun foo() {\n" +
            "    if(true) {} else <caret>\n" +
            "    foo()\n" +
            "}",

            "fun foo() {\n" +
            "    if(true) {} else {<caret>\n" +
            "    foo()\n" +
            "}"
    )

    fun testDoNotAutoCloseBraceInUnfinishedTryOnOtherLine() = doCharTypeTest(
            '{',

            "fun foo() {\n" +
            "    try <caret>\n" +
            "    foo()\n" +
            "}",

            "fun foo() {\n" +
            "    try {<caret>\n" +
            "    foo()\n" +
            "}"
    )

    fun testDoNotAutoCloseBraceInUnfinishedIfSurroundOnNewLine() = doCharTypeTest(
            '{',

            "fun foo() {\n" +
            "    if(true)\n" +
            "        <caret>\n" +
            "    foo()\n" +
            "}",

            "fun foo() {\n" +
            "    if(true)\n" +
            "    {<caret>\n" +
            "    foo()\n" +
            "}"
    )

    fun testDoNotAutoCloseBraceInUnfinishedElseSurroundOnNewLine() = doCharTypeTest(
            '{',

            "fun foo() {\n" +
            "    if(true) {} else\n" +
            "        <caret>\n" +
            "    foo()\n" +
            "}",

            "fun foo() {\n" +
            "    if(true) {} else\n" +
            "    {<caret>\n" +
            "    foo()\n" +
            "}"
    )

    fun testDoNotAutoCloseBraceInUnfinishedTryOnNewLine() = doCharTypeTest(
            '{',

            "fun foo() {\n" +
            "    try\n" +
            "        <caret>\n" +
            "    foo()\n" +
            "}",

            "fun foo() {\n" +
            "    try\n" +
            "    {<caret>\n" +
            "    foo()\n" +
            "}"
    )

    fun testAutoCloseBraceInsideFor() = doCharTypeTest(
            '{',

            "fun foo() {\n" +
            "    for (elem in some.filter <caret>) {\n" +
            "    }\n" +
            "}",

            "fun foo() {\n" +
            "    for (elem in some.filter {<caret>}) {\n" +
            "    }\n" +
            "}"
    )

    fun testAutoCloseBraceInsideForAfterCloseParen() = doCharTypeTest(
            '{',

            "fun foo() {\n" +
            "    for (elem in some.foo(true) <caret>) {\n" +
            "    }\n" +
            "}",

            "fun foo() {\n" +
            "    for (elem in some.foo(true) {<caret>}) {\n" +
            "    }\n" +
            "}"
    )

    fun testAutoCloseBraceBeforeIf() = doCharTypeTest(
            '{',

            "fun foo() {\n" +
            "    <caret>if (true) {}\n" +
            "}",

            "fun foo() {\n" +
            "    {<caret>if (true) {}\n" +
            "}"
    )

    fun testAutoCloseBraceInIfCondition() = doCharTypeTest(
            '{',

            "fun foo() {\n" +
            "    if (some.hello (12) <caret>)\n" +
            "}",

            "fun foo() {\n" +
            "    if (some.hello (12) {<caret>})\n" +
            "}"
    )

    fun testAutoInsertParenInStringLiteral() = doCharTypeTest(
            '(',
            """fun f() { println("$dollar{f<caret>}") }""",
            """fun f() { println("$dollar{f(<caret>)}") }"""
    )

    fun testAutoInsertParenInCode() = doCharTypeTest(
            '(',
            """fun f() { val a = f<caret> }""",
            """fun f() { val a = f(<caret>) }"""
    )

    fun testSplitStringByEnter() = doCharTypeTest(
            '\n',
            """val s = "foo<caret>bar"""",
            "val s = \"foo\" +\n" +
            "        \"bar\""
    )

    fun testSplitStringByEnterEmpty() = doCharTypeTest(
            '\n',
            """val s = "<caret>"""",
            "val s = \"\" +\n" +
            "        \"\""
    )

    fun testSplitStringByEnterBeforeEscapeSequence() = doCharTypeTest(
            '\n',
            """val s = "foo<caret>\nbar"""",
            "val s = \"foo\" +\n" +
            "        \"\\nbar\""
    )

    fun testSplitStringByEnterBeforeSubstitution() = doCharTypeTest(
            '\n',
            """val s = "foo<caret>${dollar}bar"""",
            "val s = \"foo\" +\n" +
            "        \"${dollar}bar\""
    )

    fun testSplitStringByEnterAddParentheses() = doCharTypeTest(
            '\n',
            """val l = "foo<caret>bar".length()""",
            "val l = (\"foo\" +\n" +
            "        \"bar\").length()"
    )

    fun testSplitStringByEnterExistingParentheses() = doCharTypeTest(
            '\n',
            """val l = ("asdf" + "foo<caret>bar").length()""",
            "val l = (\"asdf\" + \"foo\" +\n" +
            "        \"bar\").length()"
    )

    fun testTypeLtInFunDeclaration() {
        doLtGtTest("fun <caret>")
    }

    fun testTypeLtInOngoingConstructorCall() {
        doLtGtTest("fun test() { Collection<caret> }")
    }

    fun testTypeLtInClassDeclaration() {
        doLtGtTest("class Some<caret> {}")
    }

    fun testTypeLtInPropertyType() {
        doLtGtTest("val a: List<caret> ")
    }

    fun testTypeLtInExtensionFunctionReceiver() {
        doLtGtTest("fun <T> Collection<caret> ")
    }

    fun testTypeLtInFunParam() {
        doLtGtTest("fun some(a : HashSet<caret>)")
    }

    fun testTypeLtInFun() {
        doLtGtTestNoAutoClose("fun some() { <<caret> }")
    }

    fun testTypeLtInLess() {
        doLtGtTestNoAutoClose("fun some() { val a = 12; a <<caret> }")
    }

    fun testColonOfSuperTypeList() {
        doCharTypeTest(
                ':',
                """
                |open class A
                |class B
                |<caret>
                """,
                """
                |open class A
                |class B
                |    :<caret>
                """)
    }

    fun testColonOfSuperTypeListInObject() {
        doCharTypeTest(
                ':',
                """
                |interface A
                |object B
                |<caret>
                """,
                """
                |interface A
                |object B
                |    :<caret>
                """)
    }

    fun testColonOfSuperTypeListInCompanionObject() {
        doCharTypeTest(
                ':',
                """
                |interface A
                |class B {
                |    companion object
                |    <caret>
                |}
                """,
                """
                |interface A
                |class B {
                |    companion object
                |        :<caret>
                |}
                """)
    }

    fun testColonOfSuperTypeListBeforeBody() {
        doCharTypeTest(
                ':',
                """
                |open class A
                |class B
                |<caret> {
                |}
                """,
                """
                |open class A
                |class B
                |    :<caret> {
                |}
                """)
    }

    fun testColonOfSuperTypeListNotNullIndent() {
        doCharTypeTest(
                ':',
                """
                |fun test() {
                |    open class A
                |    class B
                |    <caret>
                |}
                """,
                """
                |fun test() {
                |    open class A
                |    class B
                |        :<caret>
                |}
                """)
    }

    fun testChainCallContinueWithDot() {
        doCharTypeTest(
                '.',
                """
                |class Test{ fun test() = this }
                |fun some() {
                |    Test()
                |    <caret>
                |}
                """,
                """
                |class Test{ fun test() = this }
                |fun some() {
                |    Test()
                |            .<caret>
                |}
                """)
    }

    fun testChainCallContinueWithSafeCall() {
        doCharTypeTest(
                '.',
                """
                |class Test{ fun test() = this }
                |fun some() {
                |    Test()
                |    ?<caret>
                |}
                """,
                """
                |class Test{ fun test() = this }
                |fun some() {
                |    Test()
                |            ?.<caret>
                |}
                """)
    }

    fun testSpaceAroundRange() {
        doCharTypeTest(
                '.',
                """
                | val test = 1 <caret>
                """,
                """
                | val test = 1 .<caret>
                """
        )
    }

    fun testIndentBeforeElseWithBlock() {
        doCharTypeTest(
                '\n',
                """
                |fun test(b: Boolean) {
                |    if (b) {
                |    }<caret>
                |    else if (!b) {
                |    }
                |}
                """,
                """
                |fun test(b: Boolean) {
                |    if (b) {
                |    }
                |    <caret>
                |    else if (!b) {
                |    }
                |}
                """
        )
    }

    fun testIndentBeforeElseWithoutBlock() {
        doCharTypeTest(
                '\n',
                """
                |fun test(b: Boolean) {
                |    if (b)
                |        foo()<caret>
                |    else {
                |    }
                |}
                """,
                """
                |fun test(b: Boolean) {
                |    if (b)
                |        foo()
                |    <caret>
                |    else {
                |    }
                |}
                """
        )
    }

    fun testIndentOnFinishedVariableEndAfterEquals() {
        doCharTypeTest(
                '\n',
                """
                |fun test() {
                |    val a =<caret>
                |    foo()
                |}
                """,
                """
                |fun test() {
                |    val a =
                |            <caret>
                |    foo()
                |}
                """
        )
    }

    fun testIndentNotFinishedVariableEndAfterEquals() {
        doCharTypeTest(
                '\n',
                """
                |fun test() {
                |    val a =<caret>
                |}
                """,
                """
                |fun test() {
                |    val a =
                |            <caret>
                |}
                """
        )
    }

    fun testSmartEnterWithTabsOnConstructorParameters() {
        doCharTypeTest(
                '\n',
                """
                |class A(
                |		a: Int,<caret>
                |)
                """,
                """
                |class A(
                |		a: Int,
                |		<caret>
                |)
                """,
                enableSmartEnterWithTabs()
        )
    }

    fun testSmartEnterWithTabsInMethodParameters() {
        doCharTypeTest(
                '\n',
                """
                |fun method(
                |         arg1: String,<caret>
                |) {}
                """,
                """
                |fun method(
                |         arg1: String,
                |         <caret>
                |) {}
                """,
                enableSmartEnterWithTabs()
        )
    }

    fun testAutoIndentInWhenClause() {
        doCharTypeTest(
            '\n',
            """
            |fun test() {
            |    when (2) {
            |        is Int -><caret>
            |    }
            |}
            """,
            """
            |fun test() {
            |    when (2) {
            |        is Int ->
            |            <caret>
            |    }
            |}
            """
        )
    }


    fun testMoveThroughGT() {
        LightPlatformCodeInsightTestCase.configureFromFileText("a.kt", "val a: List<Set<Int<caret>>>")
        EditorTestUtil.performTypingAction(LightPlatformCodeInsightTestCase.getEditor(), '>')
        EditorTestUtil.performTypingAction(LightPlatformCodeInsightTestCase.getEditor(), '>')
        checkResultByText("val a: List<Set<Int>><caret>")
    }

    fun testCharClosingQuote() {
        doCharTypeTest('\'', "val c = <caret>", "val c = ''")
    }

    private fun enableSmartEnterWithTabs(): () -> Unit = {
        val project = LightPlatformTestCase.getProject()
        val indentOptions = CodeStyleSettingsManager.getInstance(project).currentSettings.getIndentOptions(KotlinFileType.INSTANCE)
        indentOptions.USE_TAB_CHARACTER = true
        indentOptions.SMART_TABS = true
    }

    private fun doCharTypeTest(ch: Char, beforeText: String, afterText: String, settingsModifier: (() -> Unit)? = null) {
        try {
            if (settingsModifier != null) {
                settingsModifier()
            }

            LightPlatformCodeInsightTestCase.configureFromFileText("a.kt", beforeText.trimMargin())
            EditorTestUtil.performTypingAction(LightPlatformCodeInsightTestCase.getEditor(), ch)
            checkResultByText(afterText.trimMargin())
        }
        finally {
            if (settingsModifier != null) {
                val project = LightPlatformTestCase.getProject()
                CodeStyleSettingsManager.getSettings(project).clearCodeStyleSettings()
            }
        }
    }

    private fun doLtGtTestNoAutoClose(initText: String) {
        doLtGtTest(initText, false)
    }

    private fun doLtGtTest(initText: String, shouldCloseBeInsert: Boolean) {
        LightPlatformCodeInsightTestCase.configureFromFileText("a.kt", initText)

        EditorTestUtil.performTypingAction(LightPlatformCodeInsightTestCase.getEditor(), '<')
        checkResultByText(if (shouldCloseBeInsert) initText.replace("<caret>", "<<caret>>") else initText.replace("<caret>", "<<caret>"))

        EditorTestUtil.performTypingAction(LightPlatformCodeInsightTestCase.getEditor(), EditorTestUtil.BACKSPACE_FAKE_CHAR)
        checkResultByText(initText)
    }

    private fun doLtGtTest(initText: String) {
        doLtGtTest(initText, true)
    }
}
