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

import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.LightCodeInsightTestCase
import com.intellij.testFramework.LightPlatformCodeInsightTestCase

class TypedHandlerTest : LightCodeInsightTestCase() {
    val dollar = '$'

    fun testTypeStringTemplateStart(): Unit = doCharTypeTest(
            '{',
            """val x = "$<caret>" """,
            """val x = "$dollar{}" """
    )

    fun testAutoIndentRightOpenBrace(): Unit = doCharTypeTest(
            '{',

            "fun test() {\n" +
            "<caret>\n" +
            "}",

            "fun test() {\n" +
            "    {<caret>}\n" +
            "}"
    )

    fun testAutoIndentLeftOpenBrace(): Unit = doCharTypeTest(
            '{',

            "fun test() {\n" +
            "      <caret>\n" +
            "}",

            "fun test() {\n" +
            "    {<caret>}\n" +
            "}"
    )

    fun testTypeStringTemplateStartWithCloseBraceAfter(): Unit = doCharTypeTest(
            '{',
            """fun foo() { "$<caret>" }""",
            """fun foo() { "$dollar{}" }"""
    )

    fun testTypeStringTemplateStartBeforeString(): Unit = doCharTypeTest(
            '{',
            """fun foo() { "$<caret>something" }""",
            """fun foo() { "$dollar{}something" }"""
    )

    fun testKT3575(): Unit = doCharTypeTest(
            '{',
            """val x = "$<caret>]" """,
            """val x = "$dollar{}]" """
    )

    fun testAutoCloseBraceInFunctionDeclaration(): Unit = doCharTypeTest(
            '{',
            "fun foo() <caret>",
            "fun foo() {<caret>}"
    )

    fun testAutoCloseBraceInLocalFunctionDeclaration(): Unit = doCharTypeTest(
            '{',

            "fun foo() {\n" +
            "    fun bar() <caret>\n" +
            "}",

            "fun foo() {\n" +
            "    fun bar() {<caret>}\n" +
            "}"
    )

    fun testAutoCloseBraceInAssignment(): Unit = doCharTypeTest(
            '{',
            "fun foo() {\n" +
            "    val a = <caret>\n" +
            "}",

            "fun foo() {\n" +
            "    val a = {<caret>}\n" +
            "}"
    )

    fun testDoNotAutoCloseBraceInUnfinishedIfSurroundOnSameLine(): Unit = doCharTypeTest(
            '{',

            "fun foo() {\n" +
            "    if() <caret>foo()\n" +
            "}",

            "fun foo() {\n" +
            "    if() {foo()\n" +
            "}"
    )

    fun testDoNotAutoCloseBraceInUnfinishedElseSurroundOnSameLine(): Unit = doCharTypeTest(
            '{',

            "fun foo() {\n" +
            "    if(true) {} else <caret>foo()\n" +
            "}",

            "fun foo() {\n" +
            "    if(true) {} else {foo()\n" +
            "}"
    )

    fun testDoNotAutoCloseBraceInUnfinishedTryOnSameLine(): Unit = doCharTypeTest(
            '{',

            "fun foo() {\n" +
            "    try <caret>foo()\n" +
            "}",

            "fun foo() {\n" +
            "    try {foo()\n" +
            "}"
    )

    fun testDoNotAutoCloseBraceInUnfinishedCatchOnSameLine(): Unit = doCharTypeTest(
            '{',

            "fun foo() {\n" +
            "    try {} catch (e: Exception) <caret>foo()\n" +
            "}",

            "fun foo() {\n" +
            "    try {} catch (e: Exception) {foo()\n" +
            "}"
    )

    fun testDoNotAutoCloseBraceInUnfinishedFinallyOnSameLine(): Unit = doCharTypeTest(
            '{',

            "fun foo() {\n" +
            "    try {} catch (e: Exception) finally <caret>foo()\n" +
            "}",

            "fun foo() {\n" +
            "    try {} catch (e: Exception) finally {foo()\n" +
            "}"
    )

    fun testDoNotAutoCloseBraceInUnfinishedWhileSurroundOnSameLine(): Unit = doCharTypeTest(
            '{',

            "fun foo() {\n" +
            "    while() <caret>foo()\n" +
            "}",

            "fun foo() {\n" +
            "    while() {foo()\n" +
            "}"
    )

    fun testDoNotAutoCloseBraceInUnfinishedWhileSurroundOnNewLine(): Unit = doCharTypeTest(
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

    fun testDoNotAutoCloseBraceInUnfinishedIfSurroundOnOtherLine(): Unit = doCharTypeTest(
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

    fun testDoNotAutoCloseBraceInUnfinishedElseSurroundOnOtherLine(): Unit = doCharTypeTest(
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

    fun testDoNotAutoCloseBraceInUnfinishedTryOnOtherLine(): Unit = doCharTypeTest(
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

    fun testDoNotAutoCloseBraceInUnfinishedIfSurroundOnNewLine(): Unit = doCharTypeTest(
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

    fun testDoNotAutoCloseBraceInUnfinishedElseSurroundOnNewLine(): Unit = doCharTypeTest(
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

    fun testDoNotAutoCloseBraceInUnfinishedTryOnNewLine(): Unit = doCharTypeTest(
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

    fun testAutoCloseBraceInsideFor(): Unit = doCharTypeTest(
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

    fun testAutoCloseBraceInsideForAfterCloseParen(): Unit = doCharTypeTest(
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

    fun testAutoCloseBraceBeforeIf(): Unit = doCharTypeTest(
            '{',

            "fun foo() {\n" +
            "    <caret>if (true) {}\n" +
            "}",

            "fun foo() {\n" +
            "    {<caret>if (true) {}\n" +
            "}"
    )

    fun testAutoCloseBraceInIfCondition(): Unit = doCharTypeTest(
            '{',

            "fun foo() {\n" +
            "    if (some.hello (12) <caret>)\n" +
            "}",

            "fun foo() {\n" +
            "    if (some.hello (12) {<caret>})\n" +
            "}"
    )

    fun testAutoInsertParenInStringLiteral(): Unit = doCharTypeTest(
            '(',
            """fun f() { println("$dollar{f<caret>}") }""",
            """fun f() { println("$dollar{f(<caret>)}") }"""
    )

    fun testAutoInsertParenInCode(): Unit = doCharTypeTest(
            '(',
            """fun f() { val a = f<caret> }""",
            """fun f() { val a = f(<caret>) }"""
    )

    fun testSplitStringByEnter(): Unit = doCharTypeTest(
            '\n',
            """val s = "foo<caret>bar"""",
            "val s = \"foo\" +\n" +
            "        \"bar\""
    )

    fun testSplitStringByEnter_Empty(): Unit = doCharTypeTest(
            '\n',
            """val s = "<caret>"""",
            "val s = \"\" +\n" +
            "        \"\""
    )

    fun testSplitStringByEnter_BeforeEscapeSequence(): Unit = doCharTypeTest(
            '\n',
            """val s = "foo<caret>\nbar"""",
            "val s = \"foo\" +\n" +
            "        \"\\nbar\""
    )

    fun testSplitStringByEnter_BeforeSubstitution(): Unit = doCharTypeTest(
            '\n',
            """val s = "foo<caret>${dollar}bar"""",
            "val s = \"foo\" +\n" +
            "        \"${dollar}bar\""
    )

    fun testSplitStringByEnter_AddParentheses(): Unit = doCharTypeTest(
            '\n',
            """val l = "foo<caret>bar".length()""",
            "val l = (\"foo\" +\n" +
            "        \"bar\").length()"
    )

    fun testSplitStringByEnter_ExistingParentheses(): Unit = doCharTypeTest(
            '\n',
            """val l = ("asdf" + "foo<caret>bar").length()""",
            "val l = (\"asdf\" + \"foo\" +\n" +
            "        \"bar\").length()"
    )

    fun testSplitStringByEnter_TripleQuotedString(): Unit = doCharTypeTest(
            '\n',
            "val l = \"\"\"foo<caret>bar\"\"\"",
            "val l = \"\"\"foo\nbar\"\"\""
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

    fun testConvertToBody() {
        doCharTypeTest(
                '\n',
                """fun school(): Int = {<caret>239""",
                """
                |fun school(): Int {
                |    <caret>239
                |}
                """
        )
    }

    fun testNotToConvertNoReturnType() {
        doCharTypeTest(
                '\n',
                """fun school() = {<caret>239""",
                """
                |fun school() = {
                |    <caret>239
                |}
                """
        )
    }

    fun testNotConvertToBodyForLambda() {
        doCharTypeTest(
                '\n',
                """fun lambda(): (Int) -> Int = {<caret>}""",
                """
                |fun lambda(): (Int) -> Int = {
                |    <caret>
                |}
                """
        )
    }

    fun testNotConvertToBodyForLambda2() {
        doCharTypeTest(
                '\n',
                """
                |fun lambda(): (Int) -> Int = { foo -><caret>
                |fun second(): Int = 123
                """,
                """
                |fun lambda(): (Int) -> Int = { foo ->
                |    <caret>
                |fun second(): Int = 123
                """
        )
    }

    fun testPropertiesConvertToBody() {
        doCharTypeTest(
                '\n',
                """
                |class A {
                |    fun size(): Int = 0
                |    val isEmpty: Boolean
                |        get() = {<caret>this.size() == 0
                |}
                """,
                """
                |class A {
                |    fun size(): Int = 0
                |    val isEmpty: Boolean
                |        get() {
                |            <caret>this.size() == 0
                |        }
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

    private fun doCharTypeTest(ch: Char, beforeText: String, afterText: String) {
        LightPlatformCodeInsightTestCase.configureFromFileText("a.kt", beforeText.trimMargin())
        EditorTestUtil.performTypingAction(LightPlatformCodeInsightTestCase.getEditor(), ch)
        checkResultByText(afterText.trimMargin())
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
