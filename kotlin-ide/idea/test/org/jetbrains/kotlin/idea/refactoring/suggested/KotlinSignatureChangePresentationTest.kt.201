/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.suggested

import com.intellij.refactoring.suggested.SuggestedRefactoringSupport.Parameter
import com.intellij.refactoring.suggested.SuggestedRefactoringSupport.Signature
import com.intellij.refactoring.suggested.BaseSignatureChangePresentationTest

class KotlinSignatureChangePresentationTest : BaseSignatureChangePresentationTest() {
    override val refactoringSupport = KotlinSuggestedRefactoringSupport()

    private fun signature(
        name: String,
        type: String?,
        parameters: List<Parameter>,
        receiverType: String? = null
    ): Signature? {
        return Signature.create(
            name,
            type,
            parameters,
            KotlinSignatureAdditionalData(DeclarationType.FUN, receiverType)
        )
    }

    fun testAddParameters() {
        val oldSignature = signature(
            "foo",
            "String",
            listOf(Parameter(0, "p1", "Int")),
            "Any"
        )!!
        val newSignature = signature(
            "foo",
            "String",
            listOf(
                Parameter(Any(), "p0", "Any"),
                Parameter(0, "p1", "Int"),
                Parameter(Any(), "p2", "Long")
            ),
            "Any"
        )!!
        doTest(
            oldSignature,
            newSignature,
            """
                Old:
                  'fun '
                  'Any.'
                  'foo'
                  '('
                  LineBreak('', true)
                  Group:
                    'p1'
                    ': '
                    'Int'
                  LineBreak('', false)
                  ')'
                  ': String'
                New:
                  'fun '
                  'Any.'
                  'foo'
                  '('
                  LineBreak('', true)
                  Group (added):
                    'p0'
                    ': '
                    'Any'
                  ','
                  LineBreak(' ', true)
                  Group:
                    'p1'
                    ': '
                    'Int'
                  ','
                  LineBreak(' ', true)
                  Group (added):
                    'p2'
                    ': '
                    'Long'
                  LineBreak('', false)
                  ')'
                  ': String'
            """.trimIndent()
        )
    }

    fun testSwapParameters() {
        val oldSignature = signature(
            "foo",
            null,
            listOf(
                Parameter(0, "p1", "Int"),
                Parameter(1, "p2", "Long")
            )
        )!!
        val newSignature = signature(
            "foo",
            null,
            listOf(
                Parameter(1, "p2", "Long"),
                Parameter(0, "p1", "Int")
            )
        )!!
        doTest(
            oldSignature,
            newSignature,
            """
                Old:
                  'fun '
                  'foo'
                  '('
                  LineBreak('', true)
                  Group (moved):
                    'p1'
                    ': '
                    'Int'
                  ','
                  LineBreak(' ', true)
                  Group:
                    'p2'
                    ': '
                    'Long'
                  LineBreak('', false)
                  ')'
                New:
                  'fun '
                  'foo'
                  '('
                  LineBreak('', true)
                  Group:
                    'p2'
                    ': '
                    'Long'
                  ','
                  LineBreak(' ', true)
                  Group (moved):
                    'p1'
                    ': '
                    'Int'
                  LineBreak('', false)
                  ')'
              """.trimIndent()
        )
    }

    fun testMoveParameter() {
        val oldSignature = signature(
            "foo",
            null,
            listOf(
                Parameter(0, "p1", "Int"),
                Parameter(1, "p2", "Long"),
                Parameter(2, "p3", "Any")
            )
        )!!
        val newSignature = signature(
            "foo",
            null,
            listOf(
                Parameter(1, "p2", "Long"),
                Parameter(2, "p3", "Any"),
                Parameter(0, "p1", "Int")
            )
        )!!
        doTest(
            oldSignature,
            newSignature,
            """
                Old:
                  'fun '
                  'foo'
                  '('
                  LineBreak('', true)
                  Group (moved):
                    'p1'
                    ': '
                    'Int'
                  ','
                  LineBreak(' ', true)
                  Group:
                    'p2'
                    ': '
                    'Long'
                  ','
                  LineBreak(' ', true)
                  Group:
                    'p3'
                    ': '
                    'Any'
                  LineBreak('', false)
                  ')'
                New:
                  'fun '
                  'foo'
                  '('
                  LineBreak('', true)
                  Group:
                    'p2'
                    ': '
                    'Long'
                  ','
                  LineBreak(' ', true)
                  Group:
                    'p3'
                    ': '
                    'Any'
                  ','
                  LineBreak(' ', true)
                  Group (moved):
                    'p1'
                    ': '
                    'Int'
                  LineBreak('', false)
                  ')'
              """.trimIndent()
        )
    }

    fun testReorderParameters() {
        val oldSignature = signature(
            "foo",
            null,
            listOf(
                Parameter(0, "p1", "Int"),
                Parameter(1, "p2", "Long"),
                Parameter(2, "p3", "Any"),
                Parameter(3, "p4", "Any")
            )
        )!!
        val newSignature = signature(
            "foo",
            null,
            listOf(
                Parameter(1, "p2", "Long"),
                Parameter(3, "p4", "Any"),
                Parameter(2, "p3", "Any"),
                Parameter(0, "p1", "Int")
            )
        )!!
        doTest(
            oldSignature,
            newSignature,
            """
                Old:
                  'fun '
                  'foo'
                  '('
                  LineBreak('', true)
                  Group (id = 0, moved):
                    'p1'
                    ': '
                    'Int'
                  ','
                  LineBreak(' ', true)
                  Group:
                    'p2'
                    ': '
                    'Long'
                  ','
                  LineBreak(' ', true)
                  Group (id = 2, moved):
                    'p3'
                    ': '
                    'Any'
                  ','
                  LineBreak(' ', true)
                  Group:
                    'p4'
                    ': '
                    'Any'
                  LineBreak('', false)
                  ')'
                New:
                  'fun '
                  'foo'
                  '('
                  LineBreak('', true)
                  Group:
                    'p2'
                    ': '
                    'Long'
                  ','
                  LineBreak(' ', true)
                  Group:
                    'p4'
                    ': '
                    'Any'
                  ','
                  LineBreak(' ', true)
                  Group (id = 2, moved):
                    'p3'
                    ': '
                    'Any'
                  ','
                  LineBreak(' ', true)
                  Group (id = 0, moved):
                    'p1'
                    ': '
                    'Int'
                  LineBreak('', false)
                  ')'
              """.trimIndent()
        )
    }

    fun testChangeFunctionName() {
        val oldSignature = signature("foo", null, emptyList())!!
        val newSignature = signature("bar", null, emptyList())!!
        doTest(
            oldSignature,
            newSignature,
            """
                Old:
                  'fun '
                  'foo' (modified)
                  '('
                  LineBreak('', false)
                  ')'
                New:
                  'fun '
                  'bar' (modified)
                  '('
                  LineBreak('', false)
                  ')'
            """.trimIndent()
        )
    }

    fun testChangeReturnType() {
        val oldSignature = signature("foo", "Any", emptyList())!!
        val newSignature = signature("foo", "String", emptyList())!!
        doTest(
            oldSignature,
            newSignature,
            """
                Old:
                  'fun '
                  'foo'
                  '('
                  LineBreak('', false)
                  ')'
                  ': '
                  'Any' (modified)
                New:
                  'fun '
                  'foo'
                  '('
                  LineBreak('', false)
                  ')'
                  ': '
                  'String' (modified)
            """.trimIndent()
        )
    }

    fun testAddReturnType() {
        val oldSignature = signature("foo", null, emptyList())!!
        val newSignature = signature("foo", "String", emptyList())!!
        doTest(
            oldSignature,
            newSignature,
            """
                Old:
                  'fun '
                  'foo'
                  '('
                  LineBreak('', false)
                  ')'
                New:
                  'fun '
                  'foo'
                  '('
                  LineBreak('', false)
                  ')'
                  ': String' (added)
                """.trimIndent()
        )
    }

    fun testRemoveReturnType() {
        val oldSignature = signature("foo", "Any", emptyList())!!
        val newSignature = signature("foo", null, emptyList())!!
        doTest(
            oldSignature,
            newSignature,
            """
                Old:
                  'fun '
                  'foo'
                  '('
                  LineBreak('', false)
                  ')'
                  ': Any' (removed)
                New:
                  'fun '
                  'foo'
                  '('
                  LineBreak('', false)
                  ')'
            """.trimIndent()
        )
    }

    fun testChangeParameterName() {
        val oldSignature = signature(
            "foo",
            null,
            listOf(
                Parameter(0, "p1", "Int"),
                Parameter(1, "p2", "Long")
            )
        )!!
        val newSignature = signature(
            "foo",
            null,
            listOf(
                Parameter(0, "p1New", "Int"),
                Parameter(1, "p2", "Long")
            )
        )!!
        doTest(
            oldSignature,
            newSignature,
            """
                Old:
                  'fun '
                  'foo'
                  '('
                  LineBreak('', true)
                  Group:
                    'p1' (modified)
                    ': '
                    'Int'
                  ','
                  LineBreak(' ', true)
                  Group:
                    'p2'
                    ': '
                    'Long'
                  LineBreak('', false)
                  ')'
                New:
                  'fun '
                  'foo'
                  '('
                  LineBreak('', true)
                  Group:
                    'p1New' (modified)
                    ': '
                    'Int'
                  ','
                  LineBreak(' ', true)
                  Group:
                    'p2'
                    ': '
                    'Long'
                  LineBreak('', false)
                  ')'
""".trimIndent()
        )
    }

    fun testChangeTwoParameterNames() {
        val oldSignature = signature(
            "foo",
            null,
            listOf(
                Parameter(0, "p1", "Int"),
                Parameter(1, "p2", "Long")
            )
        )!!
        val newSignature = signature(
            "foo",
            null,
            listOf(
                Parameter(0, "p1New", "Int"),
                Parameter(1, "p2New", "Long")
            )
        )!!
        doTest(
            oldSignature,
            newSignature,
            """
                Old:
                  'fun '
                  'foo'
                  '('
                  LineBreak('', true)
                  Group:
                    'p1' (modified)
                    ': '
                    'Int'
                  ','
                  LineBreak(' ', true)
                  Group:
                    'p2' (modified)
                    ': '
                    'Long'
                  LineBreak('', false)
                  ')'
                New:
                  'fun '
                  'foo'
                  '('
                  LineBreak('', true)
                  Group:
                    'p1New' (modified)
                    ': '
                    'Int'
                  ','
                  LineBreak(' ', true)
                  Group:
                    'p2New' (modified)
                    ': '
                    'Long'
                  LineBreak('', false)
                  ')'
            """.trimIndent()
        )
    }

    fun testMoveAndRenameParameter() {
        val oldSignature = signature(
            "foo",
            null,
            listOf(
                Parameter(0, "p1", "Int"),
                Parameter(1, "p2", "Long"),
                Parameter(2, "p3", "Any")
            )
        )!!
        val newSignature = signature(
            "foo",
            null,
            listOf(
                Parameter(1, "p2", "Long"),
                Parameter(2, "p3", "Any"),
                Parameter(0, "p1New", "Int")
            )
        )!!
        doTest(
            oldSignature,
            newSignature,
            """
                Old:
                  'fun '
                  'foo'
                  '('
                  LineBreak('', true)
                  Group (moved):
                    'p1' (modified)
                    ': '
                    'Int'
                  ','
                  LineBreak(' ', true)
                  Group:
                    'p2'
                    ': '
                    'Long'
                  ','
                  LineBreak(' ', true)
                  Group:
                    'p3'
                    ': '
                    'Any'
                  LineBreak('', false)
                  ')'
                New:
                  'fun '
                  'foo'
                  '('
                  LineBreak('', true)
                  Group:
                    'p2'
                    ': '
                    'Long'
                  ','
                  LineBreak(' ', true)
                  Group:
                    'p3'
                    ': '
                    'Any'
                  ','
                  LineBreak(' ', true)
                  Group (moved):
                    'p1New' (modified)
                    ': '
                    'Int'
                  LineBreak('', false)
                  ')'
              """.trimIndent()
        )
    }

    fun testMoveParameterAndChangeFunctionName() {
        val oldSignature = signature(
            "foo",
            null,
            listOf(
                Parameter(0, "p1", "Int"),
                Parameter(1, "p2", "Long"),
                Parameter(2, "p3", "Any")
            )
        )!!
        val newSignature = signature(
            "fooNew",
            null,
            listOf(
                Parameter(1, "p2", "Long"),
                Parameter(2, "p3", "Any"),
                Parameter(0, "p1", "Int")
            )
        )!!
        doTest(
            oldSignature,
            newSignature,
            """
                Old:
                  'fun '
                  'foo' (modified)
                  '('
                  LineBreak('', true)
                  Group (id = 0, moved):
                    'p1'
                    ': '
                    'Int'
                  ','
                  LineBreak(' ', true)
                  Group:
                    'p2'
                    ': '
                    'Long'
                  ','
                  LineBreak(' ', true)
                  Group:
                    'p3'
                    ': '
                    'Any'
                  LineBreak('', false)
                  ')'
                New:
                  'fun '
                  'fooNew' (modified)
                  '('
                  LineBreak('', true)
                  Group:
                    'p2'
                    ': '
                    'Long'
                  ','
                  LineBreak(' ', true)
                  Group:
                    'p3'
                    ': '
                    'Any'
                  ','
                  LineBreak(' ', true)
                  Group (id = 0, moved):
                    'p1'
                    ': '
                    'Int'
                  LineBreak('', false)
                  ')'
              """.trimIndent()
        )
    }

    fun testAddReceiver() {
        val oldSignature = signature(
            "foo",
            null,
            emptyList()
        )!!
        val newSignature = signature(
            "foo",
            null,
            emptyList(),
            "Any"
        )!!
        doTest(
            oldSignature,
            newSignature,
            """
                Old:
                  'fun '
                  'foo'
                  '('
                  LineBreak('', false)
                  ')'
                New:
                  'fun '
                  'Any.' (added)
                  'foo'
                  '('
                  LineBreak('', false)
                  ')'
              """.trimIndent()
        )
    }

    fun testRemoveReceiver() {
        val oldSignature = signature(
            "foo",
            null,
            emptyList(),
            "Any"
        )!!
        val newSignature = signature(
            "foo",
            null,
            emptyList()
        )!!
        doTest(
            oldSignature,
            newSignature,
            """
                Old:
                  'fun '
                  'Any.' (removed)
                  'foo'
                  '('
                  LineBreak('', false)
                  ')'
                New:
                  'fun '
                  'foo'
                  '('
                  LineBreak('', false)
                  ')'
              """.trimIndent()
        )
    }

    fun testChangeReceiverType() {
        val oldSignature = signature(
            "foo",
            null,
            emptyList(),
            "Any"
        )!!
        val newSignature = signature(
            "foo",
            null,
            emptyList(),
            "String"
        )!!
        doTest(
            oldSignature,
            newSignature,
            """
                Old:
                  'fun '
                  'Any' (modified)
                  '.'
                  'foo'
                  '('
                  LineBreak('', false)
                  ')'
                New:
                  'fun '
                  'String' (modified)
                  '.'
                  'foo'
                  '('
                  LineBreak('', false)
                  ')'
              """.trimIndent()
        )
    }

    fun testChangeDefaultValues() {
        val oldSignature = signature(
            "foo",
            null,
            listOf(
                Parameter(0, "p1", "Int", KotlinParameterAdditionalData("1", "")),
                Parameter(1, "p2", "Int", KotlinParameterAdditionalData("2", "")),
                Parameter(2, "p3", "Int", KotlinParameterAdditionalData("3", "")),
                Parameter(3, "p4", "Int")
            )
        )!!
        val newSignature = signature(
            "foo",
            null,
            listOf(
                Parameter(0, "p1", "Int", KotlinParameterAdditionalData("1", "")),
                Parameter(1, "p2", "Int", KotlinParameterAdditionalData("22", "")),
                Parameter(2, "p3", "Int"),
                Parameter(3, "p4", "Int", KotlinParameterAdditionalData("4", ""))
            )
        )!!
        doTest(
            oldSignature,
            newSignature,
            """
                Old:
                  'fun '
                  'foo'
                  '('
                  LineBreak('', true)
                  Group:
                    'p1'
                    ': '
                    'Int'
                    ' = '
                    '1'
                  ','
                  LineBreak(' ', true)
                  Group:
                    'p2'
                    ': '
                    'Int'
                    ' = '
                    '2' (modified)
                  ','
                  LineBreak(' ', true)
                  Group:
                    'p3'
                    ': '
                    'Int'
                    ' = ' (removed)
                    '3' (removed)
                  ','
                  LineBreak(' ', true)
                  Group:
                    'p4'
                    ': '
                    'Int'
                  LineBreak('', false)
                  ')'
                New:
                  'fun '
                  'foo'
                  '('
                  LineBreak('', true)
                  Group:
                    'p1'
                    ': '
                    'Int'
                    ' = '
                    '1'
                  ','
                  LineBreak(' ', true)
                  Group:
                    'p2'
                    ': '
                    'Int'
                    ' = '
                    '22' (modified)
                  ','
                  LineBreak(' ', true)
                  Group:
                    'p3'
                    ': '
                    'Int'
                  ','
                  LineBreak(' ', true)
                  Group:
                    'p4'
                    ': '
                    'Int'
                    ' = ' (added)
                    '4' (added)
                  LineBreak('', false)
                  ')'
                """.trimIndent()
        )
    }
}