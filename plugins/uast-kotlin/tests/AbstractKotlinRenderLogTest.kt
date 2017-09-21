package org.jetbrains.uast.test.kotlin

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.addToStdlib.assertedCast
import org.jetbrains.uast.UDeclaration
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.test.common.RenderLogTestBase
import org.jetbrains.uast.visitor.UastVisitor
import java.io.File

abstract class AbstractKotlinRenderLogTest : AbstractKotlinUastTest(), RenderLogTestBase {
    override fun getTestFile(testName: String, ext: String) =
            File(File(TEST_KOTLIN_MODEL_DIR, testName).canonicalPath + '.' + ext)

    override fun check(testName: String, file: UFile) {
        super.check(testName, file)
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