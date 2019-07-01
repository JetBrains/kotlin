/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.editor

import com.intellij.application.options.CodeStyle
import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.LightCodeInsightTestCase
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import com.intellij.testFramework.LightPlatformTestCase
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.formatter.KotlinStyleGuideCodeStyle
import org.jetbrains.kotlin.idea.formatter.ktCodeStyleSettings
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.runner.RunWith

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class TypedHandlerTest : LightCodeInsightTestCase() {
    private val dollar = '$'

    fun testTypeStringTemplateStart() = doTypeTest(
            '{',
            """val x = "$<caret>" """,
            """val x = "$dollar{}" """
    )

    fun testAutoIndentRightOpenBrace() = doTypeTest(
            '{',

            "fun test() {\n" +
            "<caret>\n" +
            "}",

            "fun test() {\n" +
            "    {<caret>}\n" +
            "}"
    )

    fun testAutoIndentLeftOpenBrace() = doTypeTest(
            '{',

            "fun test() {\n" +
            "      <caret>\n" +
            "}",

            "fun test() {\n" +
            "    {<caret>}\n" +
            "}"
    )

    fun testTypeStringTemplateStartWithCloseBraceAfter() = doTypeTest(
            '{',
            """fun foo() { "$<caret>" }""",
            """fun foo() { "$dollar{}" }"""
    )

    fun testTypeStringTemplateStartBeforeStringWithExistingDollar() = doTypeTest(
            '{',
            """fun foo() { "$<caret>something" }""",
            """fun foo() { "$dollar{something" }"""
    )

    fun testTypeStringTemplateStartBeforeStringWithNoDollar() = doTypeTest(
        "$dollar{",
        """fun foo() { "<caret>something" }""",
        """fun foo() { "$dollar{<caret>}something" }"""
    )

    fun testTypeStringTemplateWithUnmatchedBrace() = doTypeTest(
        "$dollar{",
        """val a = "<caret>bar}foo"""",
        """val a = "$dollar{<caret>bar}foo""""
    )

    fun testTypeStringTemplateWithUnmatchedBraceComplex() = doTypeTest(
        "$dollar{",
        """val a = "<caret>bar + more}foo"""",
        """val a = "$dollar{<caret>}bar + more}foo""""
    )

    fun testTypeStringTemplateStartInStringWithBraceLiterals() = doTypeTest(
        "$dollar{",
        """val test = "{ code <caret>other }"""",
        """val test = "{ code $dollar{<caret>}other }""""
    )

    fun testTypeStringTemplateStartInEmptyString() = doTypeTest(
        '{',
        """fun foo() { "$<caret>" }""",
        """fun foo() { "$dollar{<caret>}" }"""
    )

    fun testKT3575() = doTypeTest(
            '{',
            """val x = "$<caret>]" """,
            """val x = "$dollar{}]" """
    )

    fun testAutoCloseRawStringInEnd() = doTypeTest(
            '"',
            """val x = ""<caret>""",
            """val x = ""${'"'}<caret>""${'"'}"""
    )

    fun testNoAutoCloseRawStringInEnd() = doTypeTest(
            '"',
            """val x = ""${'"'}<caret>""",
            """val x = ""${'"'}""""
    )

    fun testAutoCloseRawStringInMiddle() = doTypeTest(
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

    fun testNoAutoCloseBetweenMultiQuotes() = doTypeTest(
            '"',
            """val x = ""${'"'}<caret>${'"'}""/**/""",
            """val x = ""${'"'}${'"'}<caret>""/**/"""
    )

    fun testNoAutoCloseBetweenMultiQuotes1() = doTypeTest(
            '"',
            """val x = ""${'"'}"<caret>"${'"'}/**/""",
            """val x = ""${'"'}""<caret>${'"'}/**/"""
    )

    fun testNoAutoCloseAfterEscape() = doTypeTest(
        '"',
        """val x = "\""<caret>""",
        """val x = "\""${'"'}<caret>""""
    )

    fun testAutoCloseBraceInFunctionDeclaration() = doTypeTest(
            '{',
            "fun foo() <caret>",
            "fun foo() {<caret>}"
    )

    fun testAutoCloseBraceInLocalFunctionDeclaration() = doTypeTest(
            '{',

            "fun foo() {\n" +
            "    fun bar() <caret>\n" +
            "}",

            "fun foo() {\n" +
            "    fun bar() {<caret>}\n" +
            "}"
    )

    fun testAutoCloseBraceInAssignment() = doTypeTest(
            '{',
            "fun foo() {\n" +
            "    val a = <caret>\n" +
            "}",

            "fun foo() {\n" +
            "    val a = {<caret>}\n" +
            "}"
    )

    fun testDoNotAutoCloseBraceInUnfinishedIfSurroundOnSameLine() = doTypeTest(
            '{',

            "fun foo() {\n" +
            "    if() <caret>foo()\n" +
            "}",

            "fun foo() {\n" +
            "    if() {foo()\n" +
            "}"
    )

    fun testDoNotAutoCloseBraceInUnfinishedElseSurroundOnSameLine() = doTypeTest(
            '{',

            "fun foo() {\n" +
            "    if(true) {} else <caret>foo()\n" +
            "}",

            "fun foo() {\n" +
            "    if(true) {} else {foo()\n" +
            "}"
    )

    fun testDoNotAutoCloseBraceInUnfinishedTryOnSameLine() = doTypeTest(
            '{',

            "fun foo() {\n" +
            "    try <caret>foo()\n" +
            "}",

            "fun foo() {\n" +
            "    try {foo()\n" +
            "}"
    )

    fun testDoNotAutoCloseBraceInUnfinishedCatchOnSameLine() = doTypeTest(
            '{',

            "fun foo() {\n" +
            "    try {} catch (e: Exception) <caret>foo()\n" +
            "}",

            "fun foo() {\n" +
            "    try {} catch (e: Exception) {foo()\n" +
            "}"
    )

    fun testDoNotAutoCloseBraceInUnfinishedFinallyOnSameLine() = doTypeTest(
            '{',

            "fun foo() {\n" +
            "    try {} catch (e: Exception) finally <caret>foo()\n" +
            "}",

            "fun foo() {\n" +
            "    try {} catch (e: Exception) finally {foo()\n" +
            "}"
    )

    fun testDoNotAutoCloseBraceInUnfinishedWhileSurroundOnSameLine() = doTypeTest(
            '{',

            "fun foo() {\n" +
            "    while() <caret>foo()\n" +
            "}",

            "fun foo() {\n" +
            "    while() {foo()\n" +
            "}"
    )

    fun testDoNotAutoCloseBraceInUnfinishedWhileSurroundOnNewLine() = doTypeTest(
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

    fun testDoNotAutoCloseBraceInUnfinishedIfSurroundOnOtherLine() = doTypeTest(
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

    fun testDoNotAutoCloseBraceInUnfinishedElseSurroundOnOtherLine() = doTypeTest(
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

    fun testDoNotAutoCloseBraceInUnfinishedTryOnOtherLine() = doTypeTest(
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

    fun testDoNotAutoCloseBraceInUnfinishedIfSurroundOnNewLine() = doTypeTest(
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

    fun testDoNotAutoCloseBraceInUnfinishedElseSurroundOnNewLine() = doTypeTest(
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

    fun testDoNotAutoCloseBraceInUnfinishedTryOnNewLine() = doTypeTest(
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

    fun testAutoCloseBraceInsideFor() = doTypeTest(
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

    fun testAutoCloseBraceInsideForAfterCloseParen() = doTypeTest(
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

    fun testAutoCloseBraceBeforeIf() = doTypeTest(
            '{',

            "fun foo() {\n" +
            "    <caret>if (true) {}\n" +
            "}",

            "fun foo() {\n" +
            "    {<caret>if (true) {}\n" +
            "}"
    )

    fun testAutoCloseBraceInIfCondition() = doTypeTest(
            '{',

            "fun foo() {\n" +
            "    if (some.hello (12) <caret>)\n" +
            "}",

            "fun foo() {\n" +
            "    if (some.hello (12) {<caret>})\n" +
            "}"
    )
    
    fun testInsertSpaceAfterRightBraceOfNestedLambda() = doTypeTest(
        '{',
        "val t = Array(100) { Array(200) <caret>}",
        "val t = Array(100) { Array(200) {<caret>} }"
    )
    
    fun testAutoInsertParenInStringLiteral() = doTypeTest(
            '(',
            """fun f() { println("$dollar{f<caret>}") }""",
            """fun f() { println("$dollar{f(<caret>)}") }"""
    )

    fun testAutoInsertParenInCode() = doTypeTest(
            '(',
            """fun f() { val a = f<caret> }""",
            """fun f() { val a = f(<caret>) }"""
    )

    fun testSplitStringByEnter() = doTypeTest(
            '\n',
            """val s = "foo<caret>bar"""",
            "val s = \"foo\" +\n" +
            "        \"bar\""
    )

    fun testSplitStringByEnterEmpty() = doTypeTest(
            '\n',
            """val s = "<caret>"""",
            "val s = \"\" +\n" +
            "        \"\""
    )

    fun testSplitStringByEnterBeforeEscapeSequence() = doTypeTest(
            '\n',
            """val s = "foo<caret>\nbar"""",
            "val s = \"foo\" +\n" +
            "        \"\\nbar\""
    )

    fun testSplitStringByEnterBeforeSubstitution() = doTypeTest(
            '\n',
            """val s = "foo<caret>${dollar}bar"""",
            "val s = \"foo\" +\n" +
            "        \"${dollar}bar\""
    )

    fun testSplitStringByEnterAddParentheses() = doTypeTest(
            '\n',
            """val l = "foo<caret>bar".length()""",
            "val l = (\"foo\" +\n" +
            "        \"bar\").length()"
    )

    fun testSplitStringByEnterExistingParentheses() = doTypeTest(
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
        doTypeTest(
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
        doTypeTest(
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
        doTypeTest(
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
        doTypeTest(
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
        doTypeTest(
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
        doTypeTest(
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
        doTypeTest(
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

    fun testContinueWithElvis() {
        doTypeTest(
            ':',
            """
                |fun test(): Any? = null
                |fun some() {
                |    test()
                |    ?<caret>
                |}
            """,
            """
                |fun test(): Any? = null
                |fun some() {
                |    test()
                |            ?:<caret>
                |}
            """
        )
    }

    fun testContinueWithOr() {
        doTypeTest(
            '|',
            """
                |fun some() {
                |    if (true
                |    |<caret>)
                |}
            """,
            """
                |fun some() {
                |    if (true
                |            ||<caret>)
                |}
            """
        )
    }

    fun testContinueWithAnd() {
        doTypeTest(
            '&',
            """
                |fun some() {
                |    val test = true
                |    &<caret>
                |}
            """,
            """
                |fun some() {
                |    val test = true
                |            &&<caret>
                |}
            """
        )
    }

    fun testSpaceAroundRange() {
        doTypeTest(
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
        doTypeTest(
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
        doTypeTest(
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
        doTypeTest(
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
        doTypeTest(
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
        doTypeTest(
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
        doTypeTest(
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
        doTypeTest(
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

    fun testEnterInFunctionWithExpressionBody() {
        doTypeTest(
            '\n',
            """
            |fun test() =<caret>
            """,
            """
            |fun test() =
            |    <caret>
            """,
            ENABLE_KOTLIN_OFFICIAL_CODE_STYLE
        )
    }

    fun testEnterInMultiDeclaration() {
        doTypeTest(
            '\n',
            """
            |fun test() {
            |    val (a, b) =<caret>
            |}
            """,
            """
            |fun test() {
            |    val (a, b) =
            |        <caret>
            |}
            """,
            ENABLE_KOTLIN_OFFICIAL_CODE_STYLE
        )
    }

    fun testEnterInVariableDeclaration() {
        doTypeTest(
            '\n',
            """
            |val test =<caret>
            """,
            """
            |val test =
            |    <caret>
            """,
            ENABLE_KOTLIN_OFFICIAL_CODE_STYLE
        )
    }

    fun testMoveThroughGT() {
        LightPlatformCodeInsightTestCase.configureFromFileText("a.kt", "val a: List<Set<Int<caret>>>")
        EditorTestUtil.performTypingAction(LightPlatformCodeInsightTestCase.getEditor(), '>')
        EditorTestUtil.performTypingAction(LightPlatformCodeInsightTestCase.getEditor(), '>')
        checkResultByText("val a: List<Set<Int>><caret>")
    }

    fun testCharClosingQuote() {
        doTypeTest('\'', "val c = <caret>", "val c = ''")
    }

    private fun enableSmartEnterWithTabs(): () -> Unit = {
        val project = LightPlatformTestCase.getProject()
        val indentOptions = CodeStyle.getSettings(project).getIndentOptions(KotlinFileType.INSTANCE)
        indentOptions.USE_TAB_CHARACTER = true
        indentOptions.SMART_TABS = true
    }

    private fun doTypeTest(ch: Char, beforeText: String, afterText: String, settingsModifier: (() -> Unit)? = null) {
        doTypeTest(ch.toString(), beforeText, afterText, settingsModifier)
    }

    private fun doTypeTest(text: String, beforeText: String, afterText: String, settingsModifier: (() -> Unit)? = null) {
        try {
            if (settingsModifier != null) {
                settingsModifier()
            }

            LightPlatformCodeInsightTestCase.configureFromFileText("a.kt", beforeText.trimMargin())
            for (ch in text) {
                EditorTestUtil.performTypingAction(LightPlatformCodeInsightTestCase.getEditor(), ch)
            }
            checkResultByText(afterText.trimMargin())
        } finally {
            if (settingsModifier != null) {
                val project = LightPlatformTestCase.getProject()
                CodeStyle.getSettings(project).clearCodeStyleSettings()
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

    companion object {
        private val ENABLE_KOTLIN_OFFICIAL_CODE_STYLE: () -> Unit = {
            val settings = ktCodeStyleSettings(LightPlatformTestCase.getProject())?.all ?: error("No Settings")
            KotlinStyleGuideCodeStyle.apply(settings)
        }
    }
}
