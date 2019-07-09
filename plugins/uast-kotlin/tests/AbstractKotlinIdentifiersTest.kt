package org.jetbrains.uast.test.kotlin

import com.intellij.psi.PsiElement
import org.jetbrains.uast.*
import org.jetbrains.uast.test.common.UElementToParentMap
import org.jetbrains.uast.test.common.kotlin.IdentifiersTestBase
import org.jetbrains.uast.test.env.assertEqualsToFile
import java.io.File
import kotlin.test.assertNotNull


abstract class AbstractKotlinIdentifiersTest : AbstractKotlinUastTest(), IdentifiersTestBase {

    private fun getTestFile(testName: String, ext: String) =
        File(File(AbstractKotlinUastTest.TEST_KOTLIN_MODEL_DIR, testName).canonicalPath + '.' + ext)

    override fun getIdentifiersFile(testName: String): File = getTestFile(testName, "identifiers.txt")

    override fun check(testName: String, file: UFile) {
        super.check(testName, file)
        assertEqualsToFile("refNames", getTestFile(testName, "refNames.txt"), file.asRefNames())
    }
}

private fun refNameRetriever(psiElement: PsiElement): UElement? =
    when (val uElement = psiElement.toUElementOfExpectedTypes(UCallExpression::class.java, UReferenceExpression::class.java)) {
        is UReferenceExpression -> uElement.referenceNameElement
        is UCallExpression -> uElement.classReference?.referenceNameElement
        else -> null
    }?.also {
        assertNotNull(it.sourcePsi, "referenceNameElement should have physical source, origin = $psiElement")
    }

fun UFile.asRefNames() = object : UElementToParentMap(::refNameRetriever) {
    override fun renderSource(element: PsiElement): String = element.javaClass.simpleName
}.visitUFileAndGetResult(this)