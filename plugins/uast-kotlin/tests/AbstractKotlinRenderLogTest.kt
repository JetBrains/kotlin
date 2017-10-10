package org.jetbrains.uast.test.kotlin

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementVisitor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.addToStdlib.assertedCast
import org.jetbrains.uast.*
import org.jetbrains.uast.java.JavaUAnnotation
import org.jetbrains.uast.kotlin.KOTLIN_CACHED_UELEMENT_KEY
import org.jetbrains.uast.kotlin.KotlinUastLanguagePlugin
import org.jetbrains.uast.test.common.RenderLogTestBase
import org.jetbrains.uast.visitor.UastVisitor
import org.junit.Assert
import java.io.File
import java.util.*

abstract class AbstractKotlinRenderLogTest : AbstractKotlinUastTest(), RenderLogTestBase {
    override fun getTestFile(testName: String, ext: String) =
            File(File(TEST_KOTLIN_MODEL_DIR, testName).canonicalPath + '.' + ext)

    override fun check(testName: String, file: UFile) {
        check(testName, file, true)
    }

    fun check(testName: String, file: UFile, checkParentConsistency: Boolean) {
        super.check(testName, file)

        if (checkParentConsistency) {
            checkParentConsistency(file)
        }

        file.checkContainingFileForAllElements()
        file.checkJvmDeclarationsImplementations()
    }

    private fun checkParentConsistency(file: UFile) {
        val parentMap = mutableMapOf<PsiElement, String>()

        file.accept(object : UastVisitor {
            private val parentStack = Stack<UElement>()

            override fun visitElement(node: UElement): Boolean {
                val parent = node.uastParent
                if (parent == null) {
                    Assert.assertTrue("Wrong parent of $node", parentStack.empty())
                }
                else {
                    Assert.assertEquals("Wrong parent of $node", parentStack.peek(), parent)
                }
                node.psi?.let {
                    if (it !in parentMap) {
                        parentMap[it] = parentStack.reversed().joinToString { it.asLogString() }
                    }
                }
                parentStack.push(node)
                return false
            }

            override fun afterVisitElement(node: UElement) {
                super.afterVisitElement(node)
                parentStack.pop()
            }
        })

        file.psi.clearUastCaches()

        file.psi.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                val uElement = KotlinUastLanguagePlugin().convertElementWithParent(element, null)
                val expectedParents = parentMap[element]
                if (expectedParents != null) {
                    assertNotNull("Expected to be able to convert PSI element $element", uElement)
                    val parents = generateSequence(uElement!!.uastParent) { it.uastParent }.joinToString { it.asLogString() }
                    assertEquals("Inconsistent parents for ${uElement.asLogString()} (converted from $element)", expectedParents, parents)
                }
                super.visitElement(element)
            }
        })
    }

    private fun UFile.checkContainingFileForAllElements() {
        accept(object : UastVisitor {
            override fun visitElement(node: UElement): Boolean {
                if (node is PsiElement) {
                    node.containingFile.assertedCast<KtFile> { "containingFile should be KtFile for ${node.asLogString()}" }
                }

                val anchorPsi = (node as? UDeclaration)?.uastAnchor?.psi
                if (anchorPsi != null) {
                    anchorPsi.containingFile.assertedCast<KtFile> { "uastAnchor.containingFile should be KtFile for ${node.asLogString()}" }
                }

                return false
            }
        })
    }

    private fun UFile.checkJvmDeclarationsImplementations() {
        accept(object : UastVisitor {
            override fun visitElement(node: UElement): Boolean {

                val jvmDeclaration = node as? JvmDeclarationUElement
                                     ?: throw AssertionError("${node.javaClass} should implement 'JvmDeclarationUElement'")

                if (jvmDeclaration is JavaUAnnotation) return false // but actually it's strange to meet JavaUAnnotation here
                if (jvmDeclaration is UIdentifier) return false // probably should be fixed in platform to fully support in in Kotlin

                jvmDeclaration.sourcePsi?.let {
                    assertTrue("sourcePsi should be physical but ${it.javaClass} found for [${it.text}] " +
                               "for ${jvmDeclaration.javaClass}->${jvmDeclaration.uastParent?.javaClass}", it is KtElement)
                }
                jvmDeclaration.javaPsi?.let {
                    assertTrue("javaPsi should be light but ${it.javaClass} found for [${it.text}] " +
                               "for ${jvmDeclaration.javaClass}->${jvmDeclaration.uastParent?.javaClass}", it !is KtElement)
                }

                return false
            }
        })
    }
}

private fun PsiFile.clearUastCaches() {
    accept(object : PsiRecursiveElementVisitor() {
        override fun visitElement(element: PsiElement) {
            super.visitElement(element)
            element.putUserData(KOTLIN_CACHED_UELEMENT_KEY, null)
        }
    })
}
