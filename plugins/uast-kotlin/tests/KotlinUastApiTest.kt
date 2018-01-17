package org.jetbrains.uast.test.kotlin

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiModifier
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.asJava.toLightAnnotation
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getValueParameterList
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.uast.*
import org.jetbrains.uast.kotlin.KotlinUastLanguagePlugin
import org.jetbrains.uast.test.env.findElementByText
import org.jetbrains.uast.test.env.findElementByTextFromPsi
import org.jetbrains.uast.visitor.AbstractUastVisitor
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
            KtUsefulTestCase.assertInstanceOf(
                annotation.psi.cast<KtAnnotationEntry>().toLightAnnotation().toUElement(),
                UAnnotation::class.java
            )
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
            assertFalse(constructor2.uastParameters.first().annotations.any { it.qualifiedName?.startsWith("MyAnnotation") ?: false })

            val getter2 = test2.methods.find { it.name == "getBar" }!!
            getter2.annotations.single { it.qualifiedName == "MyAnnotation" }

            val setter2 = test2.methods.find { it.name == "setBar" }!!
            setter2.annotations.single { it.qualifiedName == "MyAnnotation2" }
            setter2.uastParameters.first().annotations.single { it.qualifiedName == "MyAnnotation3" }

            test2.fields.find { it.name == "bar" }!!.annotations.single { it.qualifiedName == "MyAnnotation5" }
        }
    }

    @Test fun testConvertTypeInAnnotation() {
        doTest("TypeInAnnotation") { _, file ->
            val index = file.psi.text.indexOf("Test")
            val element = file.psi.findElementAt(index)!!.getParentOfType<KtUserType>(false)!!
            assertNotNull(element.getUastParentOfType(UAnnotation::class.java))
        }
    }

    @Test fun testElvisType() {
        doTest("ElvisType") { _, file ->
            val elvisExpression = file.findElementByText<UExpression>("text ?: return")
            assertEquals("String", elvisExpression.getExpressionType()!!.presentableText)
        }
    }

    @Test fun testFindAttributeDefaultValue() {
        doTest("AnnotationParameters") { _, file ->
            val witDefaultValue = file.findElementByText<UAnnotation>("@WithDefaultValue")
            assertEquals(42, witDefaultValue.findAttributeValue("value")!!.evaluate())
            assertEquals(42, witDefaultValue.findAttributeValue(null)!!.evaluate())
        }
    }

    @Test fun testIfCondition() {
        doTest("IfStatement") { _, file ->
            val psiFile = file.psi
            val element = psiFile.findElementAt(psiFile.text.indexOf("\"abc\""))!!
            val binaryExpression = element.getParentOfType<KtBinaryExpression>(false)!!
            val uBinaryExpression = KotlinUastLanguagePlugin().convertElementWithParent(binaryExpression, null)!!
            UsefulTestCase.assertInstanceOf(uBinaryExpression.uastParent, UIfExpression::class.java)
        }
    }

    @Test
    fun testWhenStringLiteral() {
        doTest("WhenStringLiteral") { _, file ->

            file.findElementByTextFromPsi<ULiteralExpression>("abc").let { literalExpression ->
                val psi = literalExpression.psi!!
                Assert.assertTrue(psi is KtLiteralStringTemplateEntry)
                UsefulTestCase.assertInstanceOf(literalExpression.uastParent, USwitchClauseExpressionWithBody::class.java)
            }

            file.findElementByTextFromPsi<ULiteralExpression>("def").let { literalExpression ->
                val psi = literalExpression.psi!!
                Assert.assertTrue(psi is KtLiteralStringTemplateEntry)
                UsefulTestCase.assertInstanceOf(literalExpression.uastParent, USwitchClauseExpressionWithBody::class.java)
            }

            file.findElementByTextFromPsi<ULiteralExpression>("def1").let { literalExpression ->
                val psi = literalExpression.psi!!
                Assert.assertTrue(psi is KtLiteralStringTemplateEntry)
                UsefulTestCase.assertInstanceOf(literalExpression.uastParent, UBlockExpression::class.java)
            }


        }
    }

    @Test
    fun testWhenAndDestructing() {
        doTest("WhenAndDestructing") { _, file ->

            file.findElementByTextFromPsi<UExpression>("val (bindingContext, statementFilter) = arr").let { e ->
                val uBlockExpression = e.getParentOfType<UBlockExpression>()
                Assert.assertNotNull(uBlockExpression)
                val uMethod = uBlockExpression!!.getParentOfType<UMethod>()
                Assert.assertNotNull(uMethod)
            }

        }
    }

    @Test
    fun testBrokenMethodTypeResolve() {
        doTest("BrokenMethod") { _, file ->

            file.accept(object : AbstractUastVisitor() {
                override fun visitCallExpression(node: UCallExpression): Boolean {
                    node.returnType
                    return false
                }
            })
        }
    }

    @Test
    fun testSimpleAnnotated() {
        doTest("SimpleAnnotated") { _, file ->
            file.findElementByTextFromPsi<UField>("@SinceKotlin(\"1.0\")\n    val property: String = \"Mary\"").let { field ->
                val annotation = field.annotations.assertedFind("kotlin.SinceKotlin") { it.qualifiedName }
                Assert.assertEquals(annotation.findDeclaredAttributeValue("version")?.evaluateString(), "1.0")
            }
        }
    }


    fun UFile.checkUastSuperTypes(refText: String, superTypes: List<String>) {
        findElementByTextFromPsi<UClass>(refText, false).let {
            assertEquals("base classes", superTypes, it.uastSuperTypes.map { it.getQualifiedName() })
        }
    }


    @Test
    fun testSuperTypes() {
        doTest("SuperCalls") { _, file ->
            file.checkUastSuperTypes("B", listOf("A"))
            file.checkUastSuperTypes("O", listOf("A"))
            file.checkUastSuperTypes("innerObject ", listOf("A"))
            file.checkUastSuperTypes("InnerClass", listOf("A"))
            file.checkUastSuperTypes("object : A(\"textForAnon\")", listOf("A"))
        }
    }

    @Test
    fun testAnonymousSuperTypes() {
        doTest("Anonymous") { _, file ->
            file.checkUastSuperTypes("object : Runnable { override fun run() {} }", listOf("java.lang.Runnable"))
            file.checkUastSuperTypes(
                "object : Runnable, Closeable { override fun close() {} override fun run() {} }",
                listOf("java.lang.Runnable", "java.io.Closeable")
            )
            file.checkUastSuperTypes(
                "object : InputStream(), Runnable { override fun read(): Int = 0; override fun run() {} }",
                listOf("java.io.InputStream", "java.lang.Runnable")
            )
        }
    }

    @Test
    fun testLiteralArraysTypes() {
        doTest("AnnotationParameters") { _, file ->
            file.findElementByTextFromPsi<UCallExpression>("intArrayOf(1, 2, 3)").let { field ->
                Assert.assertEquals("PsiType:int[]", field.returnType.toString())
            }
            file.findElementByTextFromPsi<UCallExpression>("[1, 2, 3]").let { field ->
                Assert.assertEquals("PsiType:int[]", field.returnType.toString())
                Assert.assertEquals("PsiType:int", field.typeArguments.single().toString())
            }
            file.findElementByTextFromPsi<UCallExpression>("[\"a\", \"b\", \"c\"]").let { field ->
                Assert.assertEquals("PsiType:String[]", field.returnType.toString())
                Assert.assertEquals("PsiType:String", field.typeArguments.single().toString())
            }

        }
    }

    @Test
    fun testTypeAliases() {
        doTest("TypeAliases") { _, file ->
            val g = (file.psi as KtFile).declarations.single { it.name == "G" } as KtTypeAlias
            val originalType = g.getTypeReference()!!.typeElement as KtFunctionType
            val originalTypeParameters = originalType.parameterList.toUElement() as UDeclarationsExpression
            Assert.assertTrue((originalTypeParameters.declarations.single() as UParameter).type.isValid)
        }
    }

    @Test
    fun testNestedAnnotation() = doTest("AnnotationComplex") { _, file ->
        file.findElementByTextFromPsi<UElement>("@AnnotationArray(value = Annotation())")
            .findElementByTextFromPsi<UElement>("Annotation()")
            .sourcePsiElement
            .let { referenceExpression ->
                val convertedUAnnotation = referenceExpression
                    .cast<KtReferenceExpression>()
                    .toUElementOfType<UAnnotation>()
                        ?: throw AssertionError("haven't got annotation from $referenceExpression(${referenceExpression?.javaClass})")

                assertEquals("Annotation", convertedUAnnotation.qualifiedName)
                val lightAnnotation = convertedUAnnotation.getAsJavaPsiElement(PsiAnnotation::class.java)
                        ?: throw AssertionError("can't get lightAnnotation from $convertedUAnnotation")
                assertEquals("Annotation", lightAnnotation.qualifiedName)
            }
    }


}

fun <T, R> Iterable<T>.assertedFind(value: R, transform: (T) -> R): T = find { transform(it) == value } ?: throw AssertionError("'$value' not found, only ${this.joinToString { transform(it).toString() }}")
