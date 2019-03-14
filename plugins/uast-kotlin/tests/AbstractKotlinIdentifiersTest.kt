package org.jetbrains.uast.test.kotlin

import com.intellij.psi.PsiElement
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.test.common.UElementToParentMap
import org.jetbrains.uast.test.common.kotlin.IdentifiersTestBase
import org.jetbrains.uast.test.env.assertEqualsToFile
import org.jetbrains.uast.toUElementOfType
import java.io.File


abstract class AbstractKotlinIdentifiersTest : AbstractKotlinUastTest(), IdentifiersTestBase {

    private fun getTestFile(testName: String, ext: String) =
        File(File(AbstractKotlinUastTest.TEST_KOTLIN_MODEL_DIR, testName).canonicalPath + '.' + ext)

    override fun getIdentifiersFile(testName: String): File = getTestFile(testName, "identifiers.txt")

    override fun check(testName: String, file: UFile) {
        super.check(testName, file)
        assertEqualsToFile("refNames", getTestFile(testName, "refNames.txt"), file.asRefNames())
    }
}

fun UFile.asRefNames() = object : UElementToParentMap({ it.toUElementOfType<UReferenceExpression>()?.referenceNameElement }) {
    override fun renderSource(element: PsiElement): String = element.javaClass.simpleName
}.visitUFileAndGetResult(this)