package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.psi.*
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.codeInliner.UsageReplacementStrategy
import org.jetbrains.kotlin.idea.refactoring.inline.J2KInlineCache.Companion.findUsageReplacementStrategy
import org.jetbrains.kotlin.idea.refactoring.inline.J2KInlineCache.Companion.setUsageReplacementStrategy
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtReferenceExpression

class InlineCacheTest : LightPlatformCodeInsightTestCase() {
    fun `test valid value`() {
        val method = createJavaMethod()
        method.setUsageReplacementStrategy(strategy)
        TestCase.assertNotNull(method.findUsageReplacementStrategy(withValidation = true))
        TestCase.assertNotNull(method.findUsageReplacementStrategy(withValidation = false))
    }

    fun `test valid value after change`() {
        val method = createJavaMethod()
        method.setUsageReplacementStrategy(strategy)
        val originalText = method.text
        val body = method.body
        val javaExpression = createJavaDeclaration()
        val newElement = body?.addAfter(javaExpression, body.statements.first())
        TestCase.assertTrue(originalText != method.text)
        newElement?.delete()
        TestCase.assertTrue(originalText == method.text)

        TestCase.assertNotNull(method.findUsageReplacementStrategy(withValidation = true))
        TestCase.assertNotNull(method.findUsageReplacementStrategy(withValidation = false))
    }

    fun `test invalid value`() {
        val method = createJavaMethod()
        method.setUsageReplacementStrategy(strategy)
        val originalText = method.text
        method.body?.statements?.firstOrNull()?.delete()

        TestCase.assertTrue(originalText != method.text)
        TestCase.assertNull(method.findUsageReplacementStrategy(withValidation = true))
        TestCase.assertNotNull(method.findUsageReplacementStrategy(withValidation = false))
    }

    private fun createJavaMethod(): PsiMethod = javaFactory.createMethodFromText(
        """void dummyFunction() {
                |    int a = 4;
                |}
            """.trimMargin(),
        null,
    )

    private fun createJavaDeclaration(): PsiDeclarationStatement = javaFactory.createVariableDeclarationStatement(
        "number",
        PsiType.INT,
        javaFactory.createExpressionFromText("1 + 3", null),
    )

    private val javaFactory: PsiElementFactory get() = JavaPsiFacade.getElementFactory(project)

    private val strategy = object : UsageReplacementStrategy {
        override fun createReplacer(usage: KtReferenceExpression): (() -> KtElement?)? = TODO()
    }
}