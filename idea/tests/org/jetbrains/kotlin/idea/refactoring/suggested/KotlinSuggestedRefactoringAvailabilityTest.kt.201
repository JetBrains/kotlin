package org.jetbrains.kotlin.idea.refactoring.suggested

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.refactoring.suggested.BaseSuggestedRefactoringAvailabilityTest
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.ImportPath

class KotlinSuggestedRefactoringAvailabilityTest : BaseSuggestedRefactoringAvailabilityTest() {
    override val fileType: LanguageFileType
        get() = KotlinFileType.INSTANCE

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun testNotAvailableWithSyntaxError() {
        doTest(
            """
                fun foo(p1: Int<caret>) {
                    foo(1)
                }
                
                fun bar() {
                    foo(2)
                }
            """.trimIndent(),
            {
                myFixture.type(", p2: Any")
            },
            {
                myFixture.type(", p")
            },
            expectedAvailability = Availability.Disabled
        )
    }

    fun testInsertTrailingComma() {
        doTest(
            """
                fun foo(p1: Int<caret>) {
                    foo(1)
                }
                
                fun bar() {
                    foo(2)
                }
            """.trimIndent(),
            {
                myFixture.type(",")
            },
            expectedAvailability = Availability.NotAvailable
        )
    }

    fun testChangeNonVirtualPropertyType() {
        doTest(
            "val v: <caret>String = \"\"",
            {
                replaceTextAtCaret("String", "Any")
            },
            expectedAvailability = Availability.Disabled
        )
    }

    fun testChangeParameterTypeNonVirtual() {
        doTest(
            "fun foo(p: <caret>String) {}",
            {
                replaceTextAtCaret("String", "Any")
            },
            expectedAvailability = Availability.Disabled
        )
    }

    fun testChangeReturnTypeNonVirtual() {
        doTest(
            "fun foo(): <caret>String = \"\"",
            {
                replaceTextAtCaret("String", "Any")
            },
            expectedAvailability = Availability.Disabled
        )
    }

    fun testChangeLocalVariableType() {
        doTest(
            """
                fun foo() {
                    val local: <caret>Int
                    local = 10
                } 
            """.trimIndent(),
            {
                replaceTextAtCaret("Int", "Long")
            },
            expectedAvailability = Availability.NotAvailable
        )
    }

    fun testAddLocalVariableType() {
        doTest(
            """
                fun foo() {
                    var local<caret> = 10
                } 
            """.trimIndent(),
            {
                myFixture.type(": Long")
            },
            expectedAvailability = Availability.NotAvailable
        )
    }

    fun testTypeLocalVariableBeforeExpression() {
        doTest(
            """
            """.trimIndent(),
            {
                myFixture.type("val ")
            },
            {
                myFixture.type("cod")
            },
            {
                myFixture.type("e = ")
            },
            expectedAvailability = Availability.NotAvailable
        )
    }

    fun testChangeParameterTypeAndName() {
        doTest(
            """
                interface I {
                    fun foo(p: <caret>Int)
                } 
            """.trimIndent(),
            {
                replaceTextAtCaret("Int", "String")
                editor.caretModel.moveToOffset(editor.caretModel.offset - "p: ".length)
                replaceTextAtCaret("p", "pNew")
            },
            expectedAvailability = Availability.Available(changeSignatureAvailableTooltip("foo", "usages"))
        )
    }

    fun testRenameTwoParameters() {
        doTest(
            """
                interface I {
                    fun foo(<caret>p1: Int, p2: Int)
                }
            """.trimIndent(),
            {
                replaceTextAtCaret("p1", "p1New")
            },
            {
                editor.caretModel.moveToOffset(editor.caretModel.offset + "p1New: Int, ".length)
                replaceTextAtCaret("p2", "p2New")
            },
            expectedAvailability = Availability.Available(changeSignatureAvailableTooltip("foo", "usages"))
        )
    }

    fun testChangeParameterType() {
        doTest(
            """
                class C {
                    open fun foo(p1: <caret>Int, p2: Int) {
                    }
                }
            """.trimIndent(),
            {
                replaceTextAtCaret("Int", "Any")
            },
            expectedAvailability = Availability.Available(changeSignatureAvailableTooltip("foo", "overrides"))
        )
    }

    fun testChangeParameterTypeForAbstract() {
        doTest(
            """
                abstract class C {
                    abstract fun foo(p1: <caret>Int, p2: Int)
                }
            """.trimIndent(),
            {
                replaceTextAtCaret("Int", "Any")
            },
            expectedAvailability = Availability.Available(changeSignatureAvailableTooltip("foo", "implementations"))
        )
    }

    fun testChangeParameterTypeInInterface() {
        doTest(
            """
                interface I {
                    fun foo(p1: <caret>Int, p2: Int)
                }
            """.trimIndent(),
            {
                replaceTextAtCaret("Int", "Any")
            },
            expectedAvailability = Availability.Available(changeSignatureAvailableTooltip("foo", "implementations"))
        )
    }

    fun testChangeParameterTypeInInterfaceWithBody() {
        doTest(
            """
                interface I {
                    fun foo(p1: <caret>Int, p2: Int) {}
                }
            """.trimIndent(),
            {
                replaceTextAtCaret("Int", "Any")
            },
            expectedAvailability = Availability.Available(changeSignatureAvailableTooltip("foo", "overrides"))
        )
    }
    
    fun testChangeTypeOfPropertyWithImplementationInInterface() {
        doTest(
            """
                interface I {
                    val p: <caret>Int
                        get() = 0
                }
            """.trimIndent(),
            {
                replaceTextAtCaret("Int", "Any")
            },
            expectedAvailability = Availability.Available(changeSignatureAvailableTooltip("p", "overrides"))
        )
    }

    fun testSpecifyExplicitType() {
        doTest(
            """
                open class C {
                    open fun foo()<caret> = 1
                }
            """.trimIndent(),
            {
                myFixture.type(": Int")
            },
            expectedAvailability = Availability.Available(changeSignatureAvailableTooltip("foo", "overrides")),
            expectedAvailabilityAfterResolve = Availability.NotAvailable
        )
    }

    fun testRemoveExplicitType() {
        doTest(
            """
                open class C {
                    open fun foo(): Int<caret> = 1
                }
            """.trimIndent(),
            {
                deleteTextBeforeCaret(": Int")
            },
            expectedAvailability = Availability.Available(changeSignatureAvailableTooltip("foo", "overrides")),
            expectedAvailabilityAfterResolve = Availability.NotAvailable
        )
    }

    fun testImportNestedClass() {
        doTest(
            """
                package ppp
                
                class C {
                    class Nested
                }
                
                interface I {
                    fun foo(): <caret>C.Nested
                }
            """.trimIndent(),
            {
                addImport("ppp.C.Nested")
            },
            {
                replaceTextAtCaret("C.Nested", "Nested")
            },
            expectedAvailability = Availability.Available(changeSignatureAvailableTooltip("foo", "implementations")),
            expectedAvailabilityAfterResolve = Availability.NotAvailable
        )
    }

    fun testImportNestedClassForReceiverType() {
        doTest(
            """
                package ppp
                
                class C {
                    class Nested
                }
                
                interface I {
                    fun <caret>C.Nested.foo()
                }
            """.trimIndent(),
            {
                addImport("ppp.C.Nested")
            },
            {
                replaceTextAtCaret("C.Nested", "Nested")
            },
            expectedAvailability = Availability.Available(changeSignatureAvailableTooltip("foo", "implementations")),
            expectedAvailabilityAfterResolve = Availability.NotAvailable
        )
    }

    fun testImportNestedClassForParameterType() {
        doTest(
            """
                package ppp
                
                class C {
                    class Nested
                }
                
                interface I {
                    fun foo(p: <caret>C.Nested)
                }
            """.trimIndent(),
            {
                addImport("ppp.C.Nested")
            },
            {
                replaceTextAtCaret("C.Nested", "Nested")
            },
            expectedAvailability = Availability.Available(changeSignatureAvailableTooltip("foo", "implementations")),
            expectedAvailabilityAfterResolve = Availability.NotAvailable
        )
    }

    fun testImportAnotherType() {
        doTest(
            """
                import java.util.Date
                
                interface I {
                    fun foo(): <caret>Date
                }
            """.trimIndent(),
            {
                replaceTextAtCaret("Date", "java.sql.Date")
            },
            {
                removeImport("java.util.Date")
                addImport("java.sql.Date")
            },
            {
                replaceTextAtCaret("java.sql.Date", "Date")
            },
            expectedAvailability = Availability.NotAvailable,
            expectedAvailabilityAfterResolve = Availability.Available((changeSignatureAvailableTooltip("foo", "implementations")))
        )
    }

    private fun addImport(fqName: String) {
        (file as KtFile).importList!!.add(KtPsiFactory(project).createImportDirective(ImportPath.fromString(fqName)))
    }

    private fun removeImport(fqName: String) {
        (file as KtFile).importList!!.imports.first { it.importedFqName?.asString() == fqName }.delete()
    }
}