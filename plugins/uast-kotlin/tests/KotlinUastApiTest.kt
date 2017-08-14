package org.jetbrains.uast.test.kotlin

import com.intellij.psi.PsiModifier
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.uast.*
import org.jetbrains.uast.test.env.findElementByText
import org.jetbrains.uast.visitor.UastVisitor
import org.junit.Assert
import org.junit.Test


class KotlinUastApiTest : AbstractKotlinUastTest() {
    override fun check(testName: String, file: UFile) {
    }

    @Test fun testAnnotationParameters() {
        doTest("AnnotationParameters") { _, file ->
            val annotation = file.findElementByText<UAnnotation>("@IntRange(from = 10, to = 0)")
            assertEquals(annotation.findAttributeValue("from")?.evaluate(), 10)
            val toAttribute = annotation.findAttributeValue("to")!!
            assertEquals(toAttribute.evaluate(), 0)
            KtUsefulTestCase.assertInstanceOf(annotation.psi.toUElement(), UAnnotation::class.java)
            KtUsefulTestCase.assertInstanceOf(toAttribute.uastParent, UNamedExpression::class.java)
            KtUsefulTestCase.assertInstanceOf(toAttribute.psi.toUElement()?.uastParent, UNamedExpression::class.java)
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

    @Test fun testInterfaceMethodWithBody() {
        doTest("DefaultImpls") { _, file ->
            val bar = file.findElementByText<UMethod>("fun bar() = \"Hello!\"")
            assertFalse(bar.containingFile.text!!, bar.psi.modifierList.hasExplicitModifier(PsiModifier.DEFAULT))
            assertTrue(bar.containingFile.text!!, bar.psi.modifierList.hasModifierProperty(PsiModifier.DEFAULT))
        }
    }

    @Test fun testParameterPropertyWithAnnotation() {
        doTest("ParameterPropertyWithAnnotation") { _, file ->
            val test1 = file.classes.find { it.name == "Test1" }!!

            val constructor1 = test1.methods.find { it.name == "Test1" }!!
            assertTrue(constructor1.uastParameters.first().annotations.any { it.qualifiedName == "MyAnnotation" })

            val getter1 = test1.methods.find { it.name == "getBar" }!!
            assertFalse(getter1.annotations.any { it.qualifiedName == "MyAnnotation" })

            val setter1 = test1.methods.find { it.name == "setBar" }!!
            assertFalse(setter1.annotations.any { it.qualifiedName == "MyAnnotation" })
            assertFalse(setter1.uastParameters.first().annotations.any { it.qualifiedName == "MyAnnotation" })


            val test2 = file.classes.find { it.name == "Test2" }!!
            val constructor2 = test2.methods.find { it.name == "Test2" }!!
            assertFalse(constructor2.uastParameters.first().annotations.any { it.qualifiedName == "MyAnnotation" })

            val getter2 = test2.methods.find { it.name == "getBar" }!!
            getter2.annotations.single { it.qualifiedName == "MyAnnotation" }

            val setter2 = test2.methods.find { it.name == "setBar" }!!
            setter2.annotations.single { it.qualifiedName == "MyAnnotation" }
            setter2.uastParameters.first().annotations.single { it.qualifiedName == "MyAnnotation" }

            test2.fields.find { it.name == "bar" }!!.annotations.single { it.qualifiedName == "MyAnnotation" }
        }
    }
}
