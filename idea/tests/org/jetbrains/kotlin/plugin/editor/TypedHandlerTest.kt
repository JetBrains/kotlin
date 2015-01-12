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

package org.jetbrains.kotlin.plugin.editor

import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.LightCodeInsightTestCase
import com.intellij.testFramework.LightPlatformCodeInsightTestCase

public class TypedHandlerTest : LightCodeInsightTestCase() {
    val amp = '$'

    public fun testTypeStringTemplateStart(): Unit = doCharTypeTest(
            '{',
            """val x = "$<caret>" """,
            """val x = "$amp{}" """
    )

    public fun testAutoIndentRightOpenBrace(): Unit = doCharTypeTest(
            '{',

            "fun test() {\n" +
            "<caret>\n" +
            "}",

            "fun test() {\n" +
            "    {<caret>}\n" +
            "}"
    )

    public fun testAutoIndentLeftOpenBrace(): Unit = doCharTypeTest(
            '{',

            "fun test() {\n" +
            "      <caret>\n" +
            "}",

            "fun test() {\n" +
            "    {<caret>}\n" +
            "}"
    )

    public fun testTypeStringTemplateStartWithCloseBraceAfter(): Unit = doCharTypeTest(
            '{',
            """fun foo() { "$<caret>" }""",
            """fun foo() { "$amp{}" }"""
    )

    public fun testTypeStringTemplateStartBeforeString(): Unit = doCharTypeTest(
            '{',
            """fun foo() { "$<caret>something" }""",
            """fun foo() { "$amp{}something" }"""
    )

    public fun testKT3575(): Unit = doCharTypeTest(
            '{',
            """val x = "$<caret>]" """,
            """val x = "$amp{}]" """
    )

    public fun testAutoCloseBraceInFunctionDeclaration(): Unit = doCharTypeTest(
            '{',
            "fun foo() <caret>",
            "fun foo() {<caret>}"
    )

    public fun testAutoCloseBraceInLocalFunctionDeclaration(): Unit = doCharTypeTest(
            '{',

            "fun foo() {\n" +
            "    fun bar() <caret>\n" +
            "}",

            "fun foo() {\n" +
            "    fun bar() {<caret>}\n" +
            "}"
    )

    public fun testAutoCloseBraceInAssignment(): Unit = doCharTypeTest(
            '{',
            "fun foo() {\n" +
            "    val a = <caret>\n" +
            "}",

            "fun foo() {\n" +
            "    val a = {<caret>}\n" +
            "}"
    )

    public fun testDoNotAutoCloseBraceInUnfinishedIfSurroundOnSameLine(): Unit = doCharTypeTest(
            '{',

            "fun foo() {\n" +
            "    if() <caret>foo()\n" +
            "}",

            "fun foo() {\n" +
            "    if() {foo()\n" +
            "}"
    )

    public fun testDoNotAutoCloseBraceInUnfinishedWhileSurroundOnSameLine(): Unit = doCharTypeTest(
            '{',

            "fun foo() {\n" +
            "    while() <caret>foo()\n" +
            "}",

            "fun foo() {\n" +
            "    while() {foo()\n" +
            "}"
    )

    public fun testDoNotAutoCloseBraceInUnfinishedWhileSurroundOnNewLine(): Unit = doCharTypeTest(
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

    public fun testDoNotAutoCloseBraceInUnfinishedIfSurroundOnOtherLine(): Unit = doCharTypeTest(
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

    public fun testDoNotAutoCloseBraceInUnfinishedIfSurroundOnNewLine(): Unit = doCharTypeTest(
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

    public fun testAutoCloseBraceInsideFor(): Unit = doCharTypeTest(
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

    public fun testAutoCloseBraceInsideForAfterCloseParen(): Unit = doCharTypeTest(
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

    public fun testAutoCloseBraceBeforeIf(): Unit = doCharTypeTest(
            '{',

            "fun foo() {\n" +
            "    <caret>if (true) {}\n" +
            "}",

            "fun foo() {\n" +
            "    {<caret>if (true) {}\n" +
            "}"
    )

    public fun testAutoCloseBraceInIfCondition(): Unit = doCharTypeTest(
            '{',

            "fun foo() {\n" +
            "    if (some.hello (12) <caret>)\n" +
            "}",

            "fun foo() {\n" +
            "    if (some.hello (12) {<caret>})\n" +
            "}"
    )

    public fun testAutoInsertParenInStringLiteral(): Unit = doCharTypeTest(
            '(',
            """fun f() { println("$amp{f<caret>}") }""",
            """fun f() { println("$amp{f(<caret>)}") }"""
    )

    public fun testAutoInsertParenInCode(): Unit = doCharTypeTest(
            '(',
            """fun f() { val a = f<caret> }""",
            """fun f() { val a = f(<caret>) }"""
    )

    public fun testTypeLtInFunDeclaration() {
        doLtGtTest("fun <caret>")
    }

    public fun testTypeLtInOngoingConstructorCall() {
        doLtGtTest("fun test() { Collection<caret> }")
    }

    public fun testTypeLtInClassDeclaration() {
        doLtGtTest("class Some<caret> {}")
    }

    public fun testTypeLtInPropertyType() {
        doLtGtTest("val a: List<caret> ")
    }

    public fun testTypeLtInExtensionFunctionReceiver() {
        doLtGtTest("fun <T> Collection<caret> ")
    }

    public fun testTypeLtInFunParam() {
        doLtGtTest("fun some(a : HashSet<caret>)")
    }

    public fun testTypeLtInFun() {
        doLtGtTestNoAutoClose("fun some() { <<caret> }")
    }

    public fun testTypeLtInLess() {
        doLtGtTestNoAutoClose("fun some() { val a = 12; a <<caret> }")
    }

    public fun testMoveThroughGT() {
        LightPlatformCodeInsightTestCase.configureFromFileText("a.kt", "val a: List<Set<Int<caret>>>")
        EditorTestUtil.performTypingAction(LightPlatformCodeInsightTestCase.getEditor(), '>')
        EditorTestUtil.performTypingAction(LightPlatformCodeInsightTestCase.getEditor(), '>')
        checkResultByText("val a: List<Set<Int>><caret>")
    }

    private fun doCharTypeTest(ch: Char, beforeText: String, afterText: String) {
        LightPlatformCodeInsightTestCase.configureFromFileText("a.kt", beforeText)
        EditorTestUtil.performTypingAction(LightPlatformCodeInsightTestCase.getEditor(), ch)
        checkResultByText(afterText)
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
