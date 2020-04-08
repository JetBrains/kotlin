/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.suggested

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.executeCommand
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.psi.PsiDocumentManager
import com.intellij.refactoring.suggested.SuggestedRefactoringExecution
import com.intellij.refactoring.suggested.BaseSuggestedRefactoringTest
import com.intellij.refactoring.suggested._suggestedChangeSignatureNewParameterValuesForTests
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.test.runTest

class KotlinSuggestedRefactoringTest : BaseSuggestedRefactoringTest() {
    override val fileType: LanguageFileType
        get() = KotlinFileType.INSTANCE

    override fun setUp() {
        super.setUp()
        _suggestedChangeSignatureNewParameterValuesForTests = {
            SuggestedRefactoringExecution.NewParameterValue.Expression(KtPsiFactory(project).createExpression("default$it"))
        }
    }

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun testAddParameter() {
        ignoreErrorsAfter = true
        doTestChangeSignature(
            """
                fun foo(p1: Int<caret>) {
                    foo(1)
                }
                
                fun bar() {
                    foo(2)
                }
            """.trimIndent(),
            """
                fun foo(p1: Int, p2: Any<caret>) {
                    foo(1, default0)
                }
                
                fun bar() {
                    foo(2, default0)
                }
            """.trimIndent(),
            "usages",
            { myFixture.type(", p2: Any") }
        )
    }

    fun testRemoveParameter() {
        doTestChangeSignature(
            """
                fun foo(p1: Any?, p2: Int<caret>) {
                    foo(null, 1)
                }
                
                fun bar() {
                    foo(1, 2)
                }
            """.trimIndent(),
            """
                fun foo(p1: Any?<caret>) {
                    foo(null)
                }
                
                fun bar() {
                    foo(1)
                }
            """.trimIndent(),
            "usages",
            {
                deleteTextBeforeCaret(", p2: Int")
            }
        )
    }

    fun testReorderParameters1() {
        doTestChangeSignature(
            """
                fun foo(p1: Any?, p2: Int, p3: Boolean<caret>) {
                    foo(null, 1, true)
                }
                
                fun bar() {
                    foo(1, 2, false)
                }
            """.trimIndent(),
            """
                fun foo(p3: Boolean, p1: Any?, p2: Int) {
                    foo(true, null, 1)
                }
                
                fun bar() {
                    foo(false, 1, 2)
                }
            """.trimIndent(),
            "usages",
            { myFixture.performEditorAction(IdeActions.MOVE_ELEMENT_LEFT) },
            { myFixture.performEditorAction(IdeActions.MOVE_ELEMENT_LEFT) },
            wrapIntoCommandAndWriteAction = false
        )
    }

    fun testReorderParameters2() {
        doTestChangeSignature(
            """
                fun foo(p1: Any?, <caret>p2: Int, p3: Boolean) {
                    foo(null, 1, true)
                }
                
                fun bar() {
                    foo(1, 2, false)
                }
            """.trimIndent(),
            """
                fun foo(p2: Int, p1: Any?, p3: Boolean) {
                    foo(1, null, true)
                }
                
                fun bar() {
                    foo(2, 1, false)
                }
            """.trimIndent(),
            "usages",
            {
                myFixture.performEditorAction(IdeActions.ACTION_EDITOR_SELECT_WORD_AT_CARET)
                myFixture.performEditorAction(IdeActions.ACTION_EDITOR_SELECT_WORD_AT_CARET)
                myFixture.performEditorAction(IdeActions.ACTION_EDITOR_CUT)
            },
            {
                editor.caretModel.moveToOffset(editor.caretModel.offset - 10)
                myFixture.performEditorAction(IdeActions.ACTION_EDITOR_PASTE)
            },
            {
                myFixture.type(", ")
            },
            {
                editor.caretModel.moveToOffset(editor.caretModel.offset + 10)
                myFixture.performEditorAction(IdeActions.ACTION_EDITOR_DELETE)
                myFixture.performEditorAction(IdeActions.ACTION_EDITOR_DELETE)
            },
            wrapIntoCommandAndWriteAction = false
        )
    }

    fun testChangeParameterType() {
        doTestChangeSignature(
            """
                interface I {
                    fun foo(p: <caret>String)
                }
                
                class C : I {
                    override fun foo(p: String) {
                    }
                }
            """.trimIndent(),
            """
                interface I {
                    fun foo(p: <caret>Any)
                }
                
                class C : I {
                    override fun foo(p: Any) {
                    }
                }
            """.trimIndent(),
            "implementations",
            {
                replaceTextAtCaret("String", "Any")
            }
        )
    }

    fun testChangeParameterTypeExpectFunction() {
        ignoreErrorsBefore = true
        ignoreErrorsAfter = true
        doTestChangeSignature(
            "expect fun foo(p: <caret>String)",
            "expect fun foo(p: <caret>Any)",
            "actual declarations",
            {
                replaceTextAtCaret("String", "Any")
            }
        )
    }

    fun testUnresolvedParameterType() {
        ignoreErrorsAfter = true
        doTestChangeSignature(
            """
                interface I {
                    fun foo(p: <caret>String)
                }
                
                class C : I {
                    override fun foo(p: String) {
                    }
                }
            """.trimIndent(),
            """
                interface I {
                    fun foo(p: XXX)
                }
                
                class C : I {
                    override fun foo(p: XXX) {
                    }
                }
            """.trimIndent(),
            "implementations",
            {
                replaceTextAtCaret("String", "XXX")
            }
        )
    }

    fun testChangeParameterTypeWithImportInsertion() {
        myFixture.addFileToProject(
            "X.kt",
            """
                package xxx
                class X
            """.trimIndent()
        )

        val otherFile = myFixture.addFileToProject(
            "Other.kt",
            """
                class D : I {
                    override fun foo(p: String) {
                    }
                }
            """.trimIndent()
        )

        doTestChangeSignature(
            """
                interface I {
                    fun foo(p: <caret>String)
                }
                
                class C : I {
                    override fun foo(p: String) {
                    }
                }
            """.trimIndent(),
            """
                import xxx.X
                
                interface I {
                    fun foo(p: <caret>X)
                }
                
                class C : I {
                    override fun foo(p: X) {
                    }
                }
            """.trimIndent(),
            "implementations",
            {
                replaceTextAtCaret("String", "X")
            },
            {
                addImport("xxx.X")
            }
        )

        assertEquals(
            """
            import xxx.X
            
            class D : I {
                override fun foo(p: X) {
                }
            }
            """.trimIndent(),
            otherFile.text
        )
    }

    fun testChangeReturnType() {
        doTestChangeSignature(
            """
                interface I {
                    fun foo(): <caret>String
                }
                
                class C : I {
                    override fun foo(): String = ""
                }
            """.trimIndent(),
            """
                interface I {
                    fun foo(): <caret>Any
                }
                
                class C : I {
                    override fun foo(): Any = ""
                }
            """.trimIndent(),
            "implementations",
            {
                replaceTextAtCaret("String", "Any")
            }
        )
    }

    fun testAddReturnType() {
        ignoreErrorsAfter = true
        doTestChangeSignature(
            """
                interface I {
                    fun foo()<caret>
                }
                
                class C : I {
                    override fun foo() {}
                }
            """.trimIndent(),
            """
                interface I {
                    fun foo(): String<caret>
                }
                
                class C : I {
                    override fun foo(): String {}
                }
            """.trimIndent(),
            "implementations",
            {
                myFixture.type(": String")
            }
        )
    }

    fun testRemoveReturnType() {
        ignoreErrorsAfter = true
        doTestChangeSignature(
            """
                interface I {
                    fun foo()<caret>: String
                }
                
                class C : I {
                    override fun foo(): String = ""
                }
            """.trimIndent(),
            """
                interface I {
                    fun foo()<caret>
                }
                
                class C : I {
                    override fun foo() = ""
                }
            """.trimIndent(),
            "implementations",
            {
                deleteTextAtCaret(": String")
            }
        )
    }

    fun testRenameAndAddReturnType() {
        ignoreErrorsAfter = true
        doTestChangeSignature(
            """
                interface I {
                    fun foo<caret>()
                }
                
                class C : I {
                    override fun foo() {}
                }
            """.trimIndent(),
            """
                interface I {
                    fun foo<caret>New(): String
                }
                
                class C : I {
                    override fun fooNew(): String {}
                }
            """.trimIndent(),
            "usages",
            {
                val offset = editor.caretModel.offset
                editor.document.insertString(offset, "New")
            },
            {
                val offset = editor.caretModel.offset
                editor.document.insertString(offset + "New()".length, ": String")
            }
        )
    }

    fun testChangeParameterTypeWithImportReplaced() {
        myFixture.addFileToProject(
            "XY.kt",
            """
                package xxx
                class X
                class Y
            """.trimIndent()
        )

        val otherFile = myFixture.addFileToProject(
            "Other.kt",
            """
                import xxx.X
                
                class D : I {
                    override fun foo(p: X) {
                    }
                }
            """.trimIndent()
        )

        doTestChangeSignature(
            """
                import xxx.X

                interface I {
                    fun foo(p: <caret>X)
                }
            """.trimIndent(),
            """
                import xxx.Y
                
                interface I {
                    fun foo(p: <caret>Y)
                }
            """.trimIndent(),
            "implementations",
            {
                val offset = editor.caretModel.offset
                editor.document.replaceString(offset, offset + "X".length, "Y")
            },
            {
                addImport("xxx.Y")
                removeImport("xxx.X")
            }
        )

        assertEquals(
            """
            import xxx.Y
            
            class D : I {
                override fun foo(p: Y) {
                }
            }
            """.trimIndent(),
            otherFile.text
        )
    }

    fun testPreserveCommentsAndFormatting() {
        doTestChangeSignature(
            """
                class A : I {
                    override fun foo() {
                    }
                }

                interface I {
                    fun foo(<caret>)
                }     
            """.trimIndent(),
            """
                class A : I {
                    override fun foo(p1: Int, p2: Long, p3: Any?) {
                    }
                }

                interface I {
                    fun foo(p1: Int/*comment 1*/, p2: Long/*comment 2*/,
                    p3: Any?/*comment 3*/<caret>)
                }     
            """.trimIndent(),
            "usages",
            {
                myFixture.type("p1: Int/*comment 1*/")
            },
            {
                myFixture.type(", p2: Long/*comment 2*/")
            },
            {
                myFixture.type(",")
                myFixture.performEditorAction(IdeActions.ACTION_EDITOR_ENTER)
                myFixture.type("p3: Any?/*comment 3*/")
            },
            wrapIntoCommandAndWriteAction = false
        )
    }

    fun testParameterCompletion() {
        ignoreErrorsAfter = true
        doTestChangeSignature(
            "package ppp\nclass Abcdef\nfun foo(<caret>p: Int) { }\nfun bar() { foo(1) }",
            "package ppp\nclass Abcdef\nfun foo(abcdef: Abcdef, <caret>p: Int) { }\nfun bar() { foo(default0, 1) }",
            "usages",
            {
                executeCommand {
                    runWriteAction {
                        myFixture.type("abcde")
                        PsiDocumentManager.getInstance(project).commitAllDocuments()
                    }
                }
            },
            {
                myFixture.completeBasic()
            },
            {
                myFixture.finishLookup('\n')
            },
            {
                executeCommand {
                    runWriteAction {
                        myFixture.type(", ")
                        PsiDocumentManager.getInstance(project).commitAllDocuments()
                    }
                }
            },
            wrapIntoCommandAndWriteAction = false
        )
    }

    fun testRenameTwoParameters() {
        doTestChangeSignature(
            """
                fun foo(<caret>p1: Int, p2: String) {
                    p1.hashCode()
                    p2.hashCode()
                }
            """.trimIndent(),
            """
                fun foo(<caret>p1New: Int, p2New: String) {
                    p1New.hashCode()
                    p2New.hashCode()
                }
            """.trimIndent(),
            "usages",
            {
                val function = (file as KtFile).declarations.single() as KtFunction
                function.valueParameters[0].setName("p1New")
                function.valueParameters[1].setName("p2New")
            }
        )
    }

    fun testChangePropertyType() {
        doTestChangeSignature(
            """
                interface I {
                    var v: <caret>String
                }
                
                class C : I {
                    override var v: String = ""
                }
            """.trimIndent(),
            """
                interface I {
                    var v: <caret>Any
                }
                
                class C : I {
                    override var v: Any = ""
                }
            """.trimIndent(),
            "implementations",
            {
                replaceTextAtCaret("String", "Any")
            },
            expectedPresentation = """
                Old:
                  'var '
                  'v'
                  ': '
                  'String' (modified)
                New:
                  'var '
                  'v'
                  ': '
                  'Any' (modified)
            """.trimIndent()
        )
    }

    fun testRenameClass() {
        doTestRename(
            """
                var v: C? = null

                interface C<caret> 
            """.trimIndent(),
            """
                var v: CNew? = null

                interface CNew<caret> 
            """.trimIndent(),
            "C",
            "CNew",
            {
                myFixture.type("New")
            }
        )
    }

    fun testRenameLocalVar() {
        doTestRename(
            """
                fun foo() {
                    var v<caret> = 0
                    v++
                }
            """.trimIndent(),
            """
                fun foo() {
                    var vNew<caret> = 0
                    vNew++
                }
            """.trimIndent(),
            "v",
            "vNew",
            {
                myFixture.type("New")
            }
        )
    }

    fun testRenameComponentVar() {
        doTestRename(
            """
                data class Pair<T1, T2>(val t1: T1, val t2: T2)
                
                fun f(): Pair<String, Int> = Pair("a", 1)
                
                fun g() {
                    val (a, b<caret>) = f()
                    b.hashCode()
                }
            """.trimIndent(),
            """
                data class Pair<T1, T2>(val t1: T1, val t2: T2)

                fun f(): Pair<String, Int> = Pair("a", 1)
                
                fun g() {
                    val (a, b1<caret>) = f()
                    b1.hashCode()
                }
            """.trimIndent(),
            "b",
            "b1",
            {
                myFixture.type("1")
            }
        )
    }

    fun testRenameLoopVar() {
        doTestRename(
            """
                fun f(list: List<String>) {
                    for (s<caret> in list) {
                        s.hashCode()
                    }
                }
            """.trimIndent(),
            """
                fun f(list: List<String>) {
                    for (s1<caret> in list) {
                        s1.hashCode()
                    }
                }
            """.trimIndent(),
            "s",
            "s1",
            {
                myFixture.type("1")
            }
        )
    }

    fun testRenameParameter() {
        doTestRename(
            """
                fun foo(p<caret>: String) {
                    p.hashCode()
                }
            """.trimIndent(),
            """
                fun foo(pNew<caret>: String) {
                    pNew.hashCode()
                }
            """.trimIndent(),
            "p",
            "pNew",
            {
                myFixture.type("New")
            }
        )
    }

    fun testRenameParametersInOverrides() {
        doTestRename(
            """
                interface I {
                    fun foo(p<caret>: String)
                }
                    
                class C : I {
                    override fun foo(p: String) {
                        p.hashCode()
                    }    
                }    
            """.trimIndent(),
            """
                interface I {
                    fun foo(pNew<caret>: String)
                }
                    
                class C : I {
                    override fun foo(pNew: String) {
                        pNew.hashCode()
                    }    
                }    
            """.trimIndent(),
            "p",
            "pNew",
            {
                myFixture.type("New")
            }
        )
    }

    fun testAddPrimaryConstructorParameter() {
        ignoreErrorsAfter = true
        doTestChangeSignature(
            """
                class C(private val x: String, y: Int<caret>)

                fun foo() {
                    val c = C("a", 1)
                }    
            """.trimIndent(),
            """
                class C(private val x: String, y: Int, z: Any<caret>)

                fun foo() {
                    val c = C("a", 1, default0)
                }    
            """.trimIndent(),
            "usages",
            { myFixture.type(", z: Any") },
            expectedPresentation = """
                Old:
                  'C'
                  '('
                  LineBreak('', true)
                  Group:
                    'x'
                    ': '
                    'String'
                  ','
                  LineBreak(' ', true)
                  Group:
                    'y'
                    ': '
                    'Int'
                  LineBreak('', false)
                  ')'
                New:
                  'C'
                  '('
                  LineBreak('', true)
                  Group:
                    'x'
                    ': '
                    'String'
                  ','
                  LineBreak(' ', true)
                  Group:
                    'y'
                    ': '
                    'Int'
                  ','
                  LineBreak(' ', true)
                  Group (added):
                    'z'
                    ': '
                    'Any'
                  LineBreak('', false)
                  ')'
              """.trimIndent()
        )
    }

    fun testAddSecondaryConstructorParameter() {
        ignoreErrorsAfter = true
        doTestChangeSignature(
            """
                class C {
                    constructor(x: String<caret>)
                }

                fun foo() {
                    val c = C("a")
                }    
            """.trimIndent(),
            """
                class C {
                    constructor(x: String, y: Int<caret>)
                }

                fun foo() {
                    val c = C("a", default0)
                }    
            """.trimIndent(),
            "usages",
            { myFixture.type(", y: Int") },
            expectedPresentation = """
                Old:
                  'constructor'
                  '('
                  LineBreak('', true)
                  Group:
                    'x'
                    ': '
                    'String'
                  LineBreak('', false)
                  ')'
                New:
                  'constructor'
                  '('
                  LineBreak('', true)
                  Group:
                    'x'
                    ': '
                    'String'
                  ','
                  LineBreak(' ', true)
                  Group (added):
                    'y'
                    ': '
                    'Int'
                  LineBreak('', false)
                  ')'
            """.trimIndent()
        )
    }

    fun testAddReceiver() {
        doTestChangeSignature(
            """
                interface I {
                    fun <caret>foo()
                }
                
                class C : I {
                    override fun foo() {
                    }
                }
            """.trimIndent(),
            """
                interface I {
                    fun Int.<caret>foo()
                }
                
                class C : I {
                    override fun Int.foo() {
                    }
                }
            """.trimIndent(),
            "usages",
            { myFixture.type("Int.") },
            expectedPresentation = """
                Old:
                  'fun '
                  'foo'
                  '('
                  LineBreak('', false)
                  ')'
                New:
                  'fun '
                  'Int.' (added)
                  'foo'
                  '('
                  LineBreak('', false)
                  ')'
            """.trimIndent()
        )
    }

    fun testAddReceiverAndParameter() {
        ignoreErrorsAfter = true
        doTestChangeSignature(
            """
                fun <caret>foo() {
                }
                
                fun bar() {
                    foo()
                }
            """.trimIndent(),
            """
                fun Int.foo(o: Any<caret>) {
                }
                
                fun bar() {
                    default0.foo(default1)
                }
            """.trimIndent(),
            "usages",
            { myFixture.type("Int.") },
            { repeat("foo(".length) { myFixture.performEditorAction(IdeActions.ACTION_EDITOR_MOVE_CARET_RIGHT) } },
            { myFixture.type("o: Any") }
        )
    }

    fun testRemoveReceiver() {
        doTestChangeSignature(
            """
                fun <caret>Int.foo() {
                }
                
                fun bar() {
                    1.foo()
                }
            """.trimIndent(),
            """
                fun <caret>foo() {
                }
                
                fun bar() {
                    foo()
                }
            """.trimIndent(),
            "usages",
            {
                repeat(4) { myFixture.performEditorAction(IdeActions.ACTION_EDITOR_DELETE) }
            }
        )
    }

    fun testChangeReceiverType() {
        doTestChangeSignature(
            """
                interface I {
                    fun <caret>Int.foo()
                }
                
                class C : I {
                    override fun Int.foo() {
                    }
                }
                
                fun I.f() {
                    1.foo()
                }
            """.trimIndent(),
            """
                interface I {
                    fun Any<caret>.foo()
                }
                
                class C : I {
                    override fun Any.foo() {
                    }
                }
                
                fun I.f() {
                    1.foo()
                }
            """.trimIndent(),
            "implementations",
            {
                repeat("Int".length) { myFixture.performEditorAction(IdeActions.ACTION_EDITOR_DELETE) }
                myFixture.type("Any")
            },
            expectedPresentation = """
                Old:
                  'fun '
                  'Int' (modified)
                  '.'
                  'foo'
                  '('
                  LineBreak('', false)
                  ')'
                New:
                  'fun '
                  'Any' (modified)
                  '.'
                  'foo'
                  '('
                  LineBreak('', false)
                  ')'
            """.trimIndent()
        )
    }

    fun testChangeReceiverTypeAndRemoveParameter() {
        doTestChangeSignature(
            """
                fun <caret>Int.foo(p: Any) {}
                
                fun bar() {
                    1.foo("a")
                }
            """.trimIndent(),
            """
                fun Any.foo() {}
                
                fun bar() {
                    1.foo()
                }
            """.trimIndent(),
            "usages",
            {
                repeat("Int".length) { myFixture.performEditorAction(IdeActions.ACTION_EDITOR_DELETE) }
                myFixture.type("Any")
            },
            {
                editor.caretModel.moveToOffset(editor.caretModel.offset + ".foo(".length)
                repeat("p: Any".length) { myFixture.performEditorAction(IdeActions.ACTION_EDITOR_DELETE) }
            }
        )
    }

    fun testAddVarargParameter() {
        doTestChangeSignature(
            """
                interface I {
                    fun foo(p: Int<caret>)
                }
                    
                class C : I {
                    override fun foo(p: Int) {
                    }
                }
            """.trimIndent(),
            """
                interface I {
                    fun foo(p: Int, vararg s: String<caret>)
                }
                    
                class C : I {
                    override fun foo(p: Int, vararg s: String) {
                    }
                }
            """.trimIndent(),
            "usages",
            { myFixture.type(", vararg s: String") },
            expectedPresentation = """
                Old:
                  'fun '
                  'foo'
                  '('
                  LineBreak('', true)
                  Group:
                    'p'
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
                    'p'
                    ': '
                    'Int'
                  ','
                  LineBreak(' ', true)
                  Group (added):
                    'vararg'
                    ' '
                    's'
                    ': '
                    'String'
                  LineBreak('', false)
                  ')'
            """.trimIndent()
        )
    }

    //TODO
/*
    fun testAddVarargModifier() {
        doTestChangeSignature(
            """
                interface I {
                    fun foo(<caret>p: Int)
                }
                    
                class C : I {
                    override fun foo(p: Int) {
                    }
                }
            """.trimIndent(),
            """
                interface I {
                    fun foo(vararg <caret>p: Int)
                }
                    
                class C : I {
                    override fun foo(vararg p: Int) {
                    }
                }
            """.trimIndent(),
            { myFixture.type("vararg ") },
            expectedPresentation = """
                Old:
                  'fun '
                  'foo'
                  '('
                  LineBreak('', true)
                  Group:
                    'p'
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
                    'vararg' (added)
                    ' '
                    'p'
                    ': '
                    'Int'
                  LineBreak('', false)
                  ')'
            """.trimIndent()
        )
    }

    fun testRemoveVarargModifier() {
        doTestChangeSignature(
            """
                interface I {
                    fun foo(<caret>vararg p: Int)
                }
                    
                class C : I {
                    override fun foo(vararg p: Int) {
                    }
                }
            """.trimIndent(),
            """
                interface I {
                    fun foo(<caret>p: Int)
                }
                    
                class C : I {
                    override fun foo(p: Int) {
                    }
                }
            """.trimIndent(),
            {
                deleteStringAtCaret("vararg ")
            },
            expectedPresentation = """
                Old:
                  'fun '
                  'foo'
                  '('
                  LineBreak('', true)
                  Group:
                    'vararg' (removed)
                    ' '
                    'p'
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
                    'p'
                    ': '
                    'Int'
                  LineBreak('', false)
                  ')'
            """.trimIndent()
        )
    }
*/

    fun testSwapConstructorParameters() {
        doTestChangeSignature(
            """
                class C(
                        p1: Int,
                        p2: String<caret>
                )
                
                fun foo() {
                    C(1, "")
                }
            """.trimIndent(),
            """
                class C(
                        p2: String,
                        p1: Int
                )
                
                fun foo() {
                    C("", 1)
                }
            """.trimIndent(),
            "usages",
            {
                myFixture.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_UP_ACTION)
            }
        )
    }

    fun testChangeParameterTypeOfVirtualExtensionMethod() {
        doTestChangeSignature(
            """
                abstract class Base {
                    protected abstract fun Int.foo(p: <caret>String)
                    
                    fun bar() {
                        1.foo("a")
                    }
                }
                
                class Derived : Base() {
                    override fun Int.foo(p: String) {
                    }
                }
            """.trimIndent(),
            """
                abstract class Base {
                    protected abstract fun Int.foo(p: <caret>Any)
                    
                    fun bar() {
                        1.foo("a")
                    }
                }
                
                class Derived : Base() {
                    override fun Int.foo(p: Any) {
                    }
                }
            """.trimIndent(),
            "implementations",
            {
                replaceTextAtCaret("String", "Any")
            }
        )
    }

    fun testAddParameterToVirtualExtensionMethod() {
        ignoreErrorsAfter = true
        doTestChangeSignature(
            """
                abstract class Base {
                    protected abstract fun Int.foo(p: String<caret>)
                    
                    fun bar() {
                        1.foo("a")
                    }
                }
                
                class Derived : Base() {
                    override fun Int.foo(p: String) {
                    }
                }
            """.trimIndent(),
            """
                abstract class Base {
                    protected abstract fun Int.foo(p: String, p1: Int<caret>)
                    
                    fun bar() {
                        1.foo("a", default0)
                    }
                }
                
                class Derived : Base() {
                    override fun Int.foo(p: String, p1: Int) {
                    }
                }
            """.trimIndent(),
            "usages",
            {
                myFixture.type(", p1: Int")
            }
        )
    }
    
    fun testAddParameterWithFullyQualifiedType() {
        doTestChangeSignature(
            """
                interface I {
                    fun foo(<caret>) 
                }
                
                class C : I {
                    override fun foo() {
                    }
                }
            """.trimIndent(),
            """
                import java.io.InputStream
                
                interface I {
                    fun foo(p: java.io.InputStream) 
                }
                
                class C : I {
                    override fun foo(p: InputStream) {
                    }
                }
            """.trimIndent(),
            "usages",
            { myFixture.type("p: java.io.InputStream") }
        )
    }

    fun testAddParameterWithDefaultValue() {
        doTestChangeSignature(
            """
                interface I {
                    fun foo(p1: Int<caret>)
                }
                
                class C : I {
                    override fun foo(p1: Int) {
                    }
                }
                
                fun f(i: I) {
                    i.foo(1)
                }
            """.trimIndent(),
            """
                interface I {
                    fun foo(p1: Int, p2: Int = 10<caret>)
                }
                
                class C : I {
                    override fun foo(p1: Int, p2: Int) {
                    }
                }
                
                fun f(i: I) {
                    i.foo(1)
                }
            """.trimIndent(),
            "usages",
            {
                myFixture.type(", p2: Int = 10")
            },
            expectedPresentation = """
                Old:
                  'fun '
                  'foo'
                  '('
                  LineBreak('', true)
                  Group:
                    'p1'
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
                  ','
                  LineBreak(' ', true)
                  Group (added):
                    'p2'
                    ': '
                    'Int'
                    ' = '
                    '10'
                  LineBreak('', false)
                  ')'
            """.trimIndent()
        )
    }

    fun testReorderParameterWithDefaultValue() {
        doTestChangeSignature(
            """
                interface I {
                    fun foo(<caret>p1: Int, p2: Int = 10)
                }
                
                class C : I {
                    override fun foo(p1: Int, p2: Int) {
                    }
                }
                
                fun f(i: I) {
                    i.foo(2)
                }
            """.trimIndent(),
            """
                interface I {
                    fun foo(p2: Int = 10, <caret>p1: Int)
                }
                
                class C : I {
                    override fun foo(p2: Int, p1: Int) {
                    }
                }
                
                fun f(i: I) {
                    i.foo(p1 = 2)
                }
            """.trimIndent(),
            "usages",
            {
                myFixture.performEditorAction(IdeActions.MOVE_ELEMENT_RIGHT)
            },
            expectedPresentation = """
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
                    'Int'
                    ' = '
                    '10'
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
                    'Int'
                    ' = '
                    '10'
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

    fun testReplaceTypeWithItsAlias() {
        doTestChangeSignature(
            """
                typealias StringToUnit = (String) -> Unit
                
                interface I {
                    fun foo(): <caret>(String) -> Unit
                }
                
                class C : I {
                    override fun foo(): (String) -> Unit {
                        throw UnsupportedOperationException()
                    }
                }
            """.trimIndent(),
            """
                typealias StringToUnit = (String) -> Unit
                
                interface I {
                    fun foo(): <caret>StringToUnit
                }
                
                class C : I {
                    override fun foo(): StringToUnit {
                        throw UnsupportedOperationException()
                    }
                }
            """.trimIndent(),
            "implementations",
            {
                replaceTextAtCaret("(String) -> Unit", "StringToUnit")
            },
            expectedPresentation = """
                Old:
                  'fun '
                  'foo'
                  '('
                  LineBreak('', false)
                  ')'
                  ': '
                  '(String) -> Unit' (modified)
                New:
                  'fun '
                  'foo'
                  '('
                  LineBreak('', false)
                  ')'
                  ': '
                  'StringToUnit' (modified)
            """.trimIndent()
        )
    }

    override fun runTest() {
        runTest { super.runTest() }
    }

    private fun addImport(fqName: String) {
        (file as KtFile).importList!!.add(KtPsiFactory(project).createImportDirective(ImportPath.fromString(fqName)))
    }

    private fun removeImport(fqName: String) {
        (file as KtFile).importList!!.imports.first { it.importedFqName?.asString() == fqName }.delete()
    }
}
