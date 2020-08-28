/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.suggested

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.executeCommand
import com.intellij.openapi.fileTypes.FileType
import com.intellij.refactoring.suggested.BaseSuggestedRefactoringChangeListenerTest
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.test.runTest

class KotlinSuggestedRefactoringChangeListenerTest : BaseSuggestedRefactoringChangeListenerTest() {
    override val fileType: FileType
        get() = KotlinFileType.INSTANCE
    
    fun test1() {
        setup("fun foo(<caret>) {}")

        perform("editingStarted: 'foo()'") { myFixture.type("p") }

        perform("nextSignature: 'foo(p)'") { commitAll() }
        perform { myFixture.type(":") }
        perform { myFixture.type(" S") }
        perform { myFixture.type("tr") }
        perform("nextSignature: 'foo(p: Str)'") { commitAll() }
        perform { myFixture.type("ing") }
        perform("nextSignature: 'foo(p: String)'") { commitAll() }

        perform {
            perform { myFixture.type(", ") }
            commitAll()
        }
    }

    fun testCompletion() {
        setup("fun foo(<caret>) {}")

        perform("editingStarted: 'foo()'") { myFixture.type("p: DoubleArra") }
        perform("nextSignature: 'foo(p: DoubleArra)'", "nextSignature: 'foo(p: DoubleArray)'") { myFixture.completeBasic() }
    }

    fun testChangeOutsideSignature() {
        setup("fun foo(<caret>) {}")

        perform("editingStarted: 'foo()'") { myFixture.type("p: A") }
        perform("reset") {
            insertString(editor.document.textLength, "\nval")
        }
    }

    fun testEditOtherSignature() {
        setup("fun foo(<caret>) {}\nfun bar() = 0")

        val otherFunction = (file as KtFile).declarations[1] as KtNamedFunction
        val offset = otherFunction.valueParameterList!!.startOffset + 1
        val marker = editor.document.createRangeMarker(offset, offset)

        perform("editingStarted: 'foo()'") { myFixture.type("p: A") }
        perform("nextSignature: 'foo(p: A)'") { commitAll() }

        perform("reset", "editingStarted: 'bar()'", "nextSignature: 'bar(p1: String)'") {
            assert(marker.isValid)
            insertString(marker.startOffset, "p1: String")
            commitAll()
        }
    }

    fun testChangeInAnotherFile() {
        setup("fun foo(<caret>) {}")

        perform("editingStarted: 'foo()'") { myFixture.type("p: A") }
        perform("reset") {
            setup("")
            myFixture.type(" ")
        }
    }

    fun testAddImport() {
        setup("fun foo(<caret>) {}")

        perform("editingStarted: 'foo()'", "nextSignature: 'foo(p: Any)'") {
            myFixture.type("p: Any")
            commitAll()
        }
        perform("nextSignature: 'foo(p: Any)'", "nextSignature: 'foo(p: Any)'") {
            addImport("java.util.ArrayList")
        }
        perform("nextSignature: 'foo(p: Any, p2: String)'") {
            myFixture.type(", p2: String")
            commitAll()
        }
        perform("nextSignature: 'foo(p: Any, p2: String)'", "nextSignature: 'foo(p: Any, p2: String)'") {
            addImport("java.util.Date")
        }
    }

    fun testAddImportWithBlankLineInsertion() {
        setup(
            """
                import foo.bar
                fun foo(<caret>) {}
            """.trimIndent()
        )

        perform("editingStarted: 'foo()'", "nextSignature: 'foo(p: ArrayList)'") {
            myFixture.type("p: ArrayList")
            commitAll()
        }
        perform("nextSignature: 'foo(p: ArrayList)'", "nextSignature: 'foo(p: ArrayList)'") {
            addImport("java.util.ArrayList")
        }
        perform("nextSignature: 'foo(p: ArrayList<String>)'") {
            myFixture.type("<String>")
            commitAll()
        }
        perform("nextSignature: 'foo(p: ArrayList<String>, p2: Any)'") {
            myFixture.type(", p2: Any")
            commitAll()
        }
    }

    fun testAddImportWithBlankLinesRemoval() {
        setup(
            """
                import foo.bar
                
                
                
                fun foo(<caret>) {}
            """.trimIndent()
        )

        perform("editingStarted: 'foo()'", "nextSignature: 'foo(p: ArrayList)'") {
            myFixture.type("p: ArrayList")
            commitAll()
        }
        perform("nextSignature: 'foo(p: ArrayList)'", "nextSignature: 'foo(p: ArrayList)'") {
            addImport("java.util.ArrayList")
        }
        perform("nextSignature: 'foo(p: ArrayList<String>)'") {
            myFixture.type("<String>")
            commitAll()
        }
        perform("nextSignature: 'foo(p: ArrayList<String>, p2: Any)'") {
            myFixture.type(", p2: Any")
            commitAll()
        }
    }

    fun testReorderParameters() {
        setup("fun foo(p1: String, p2: Any, p3<caret>: Int) {}")

        perform("editingStarted: 'foo(p1: String, p2: Any, p3: Int)'") {
            myFixture.performEditorAction(IdeActions.MOVE_ELEMENT_LEFT)
        }
        perform("nextSignature: 'foo(p1: String, p3: Int, p2: Any)'") {
            myFixture.performEditorAction(IdeActions.MOVE_ELEMENT_LEFT)
        }
        perform("nextSignature: 'foo(p3: Int, p1: String, p2: Any)'") {
            commitAll()
        }
        perform("nextSignature: 'foo(p1: String, p3: Int, p2: Any)'") {
            myFixture.performEditorAction(IdeActions.MOVE_ELEMENT_RIGHT)
            commitAll()
        }
    }

    fun testAddParameterViaPsi() {
        setup("fun foo(p1: Int) {}")

        val function = (file as KtFile).declarations.single() as KtFunction
        perform(
            "editingStarted: 'foo(p1: Int)'",
            "nextSignature: 'foo(p1: Int,)'",
            "nextSignature: 'foo(p1: Int,p2: Int)'",
            "nextSignature: 'foo(p1: Int, p2: Int)'"
        ) {
            executeCommand {
                runWriteAction {
                    function.valueParameterList!!.addParameter(KtPsiFactory(project).createParameter("p2: Int"))
                }
            }
        }
    }

    fun testCommentTyping() {
        setup("fun foo(<caret>) {}")

        perform("editingStarted: 'foo()'", "nextSignature: 'foo(p1: Any)'") {
            myFixture.type("p1: Any")
            commitAll()
        }

        perform {
            myFixture.type("/*")
            commitAll()
        }

        perform {
            myFixture.type(" this is comment for parameter")
            commitAll()
        }

        perform("nextSignature: 'foo(p1: Any/* this is comment for parameter*/)'") {
            myFixture.type("*/")
            commitAll()
        }

        perform {
            myFixture.type(", p2: Int /*")
            commitAll()
        }

        perform {
            myFixture.type("this is comment for another parameter")
            commitAll()
        }

        perform("nextSignature: 'foo(p1: Any/* this is comment for parameter*/, p2: Int /*this is comment for another parameter*/)'") {
            myFixture.type("*/")
            commitAll()
        }
    }

    fun testAddReturnType() {
        setup(
            """
                interface I {
                    fun foo()<caret>
                }    
            """.trimIndent()
        )

        perform("editingStarted: 'foo()'") { myFixture.type(": String") }

        perform("nextSignature: 'foo(): String'") { commitAll() }
    }

    fun testNewLocal() {
        setup(
            """
                fun foo() {
                    <caret>
                    print(a)
                }
            """.trimIndent()
        )

        perform {
            myFixture.type("val a")
            commitAll()
            myFixture.type("bcd")
            commitAll()
        }
    }

    fun testNewFunction() {
        setup(
            """
                interface I {
                    <caret>
                }    
            """.trimIndent()
        )

        perform {
            myFixture.type("fun foo_bar123(_p1: Int)")
            commitAll()
        }
    }

    fun testNewProperty() {
        setup(
            """
                interface I {
                    <caret>
                }    
            """.trimIndent()
        )

        perform {
            myFixture.type("val prop: I")
            commitAll()

            myFixture.type("nt")
            commitAll()
        }
    }

    fun testNewLocalWithNewUsage() {
        setup(
            """
                fun foo() {
                    <caret>
                }
            """.trimIndent()
        )

        perform {
            myFixture.type("val a = 10")
            myFixture.performEditorAction(IdeActions.ACTION_EDITOR_ENTER)
            myFixture.type("print(a)")
            commitAll()
        }

        perform("editingStarted: 'a'", "nextSignature: 'abcd'") {
            val variable = file.findDescendantOfType<KtProperty>()!!
            myFixture.editor.caretModel.moveToOffset(variable.nameIdentifier!!.endOffset)
            myFixture.type("bcd")
            commitAll()
        }
    }

    fun testNewLocalBeforeExpression() {
        setup(
            """
                fun foo(p: Int) {
                    <caret>p * p
                }
            """.trimIndent()
        )

        perform {
            myFixture.type("val a")
            commitAll()
        }
        perform {
            myFixture.type("bcd = ")
            commitAll()
        }
    }

    fun testNewClassWithConstructor() {
        setup("")

        perform {
            myFixture.type("class C")
            commitAll()
        }
        perform {
            myFixture.type("(p: Int)")
            commitAll()
        }
    }

    fun testNewSecondaryConstructor() {
        setup(
            """
                class C {
                    <caret>
                }
            """.trimIndent()
        )

        perform {
            myFixture.type("constructor(p1: Int)")
            commitAll()
        }
        perform {
            myFixture.type("(, p2: String)")
            commitAll()
        }
    }

    fun testRenameComponentVar() {
        setup(
            """
                fun f() {
                    val (<caret>a, b) = f()
                }
            """.trimIndent()
        )

        perform("editingStarted: 'a'", "nextSignature: 'newa'") {
            myFixture.type("new")
            commitAll()
        }
    }

    override fun runTest() {
        runTest { super.runTest() }
    }

    private fun addImport(fqName: String) {
        executeCommand {
            runWriteAction {
                (file as KtFile).importList!!.add(KtPsiFactory(project).createImportDirective(ImportPath.fromString(fqName)))
            }
        }
    }
}
