package org.jetbrains.uast.test.kotlin

import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.uast.*
import org.jetbrains.uast.test.env.findElementByText
import org.junit.Assert
import org.junit.Test


class KotlinUastApiTest : AbstractKotlinUastTest() {
    override fun check(testName: String, file: UFile) {
    }

    @Test fun testAnnotationParameters() {
        doTest("AnnotationParameters") { _, file ->
            val annotation = file.findElementByText<UAnnotation>("@IntRange(from = 10, to = 0)")
            assertEquals(annotation.findAttributeValue("from")?.evaluate(), 10)
            assertEquals(annotation.findAttributeValue("to")?.evaluate(), 0)
        }
    }

    @Test fun testConvertStringTemplate() {
        doTest("StringTemplateInClass") { _, file ->
            val literalExpression = file.findElementByText<ULiteralExpression>("lorem")
            val psi = literalExpression.psi!!
            Assert.assertTrue(psi is KtLiteralStringTemplateEntry)
            val literalExpressionAgain = psi.toUElement()
            Assert.assertTrue(literalExpressionAgain is ULiteralExpression)

        }
    }

    @Test fun testConvertStringTemplateWithExpectedType() {
        doTest("StringTemplateWithVar") { _, file ->
            val index = file.psi.text.indexOf("foo")
            val stringTemplate = file.psi.findElementAt(index)!!.getParentOfType<KtStringTemplateExpression>(false)
            val uLiteral = stringTemplate.toUElementOfType<ULiteralExpression>()
            assertNull(uLiteral)
        }
    }

    @Test fun testNameContainingFile() {
        doTest("NameContainingFile") { _, file ->
            val foo = file.findElementByText<UClass>("class Foo")
            assertEquals(file.psi, foo.nameIdentifier!!.containingFile)

            val bar = file.findElementByText<UMethod>("fun bar() {}")
            assertEquals(file.psi, bar.nameIdentifier!!.containingFile)

            val xyzzy = file.findElementByText<UVariable>("val xyzzy: Int = 0")
            assertEquals(file.psi, xyzzy.nameIdentifier!!.containingFile)
        }
    }
}
