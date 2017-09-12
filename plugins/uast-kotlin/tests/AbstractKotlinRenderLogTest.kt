package org.jetbrains.uast.test.kotlin

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.addToStdlib.assertedCast
import org.jetbrains.uast.UDeclaration
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UFile
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
        super.check(testName, file)

        file.psi.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                KotlinUastLanguagePlugin().convertElementWithParent(element, null)
                super.visitElement(element)
            }
        })

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
                parentStack.push(node)
                return false
            }

            override fun afterVisitElement(node: UElement) {
                super.afterVisitElement(node)
                parentStack.pop()
            }
        })
        file.checkContainingFileForAllElements()
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
}
