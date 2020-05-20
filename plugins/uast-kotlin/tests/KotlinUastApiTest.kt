package org.jetbrains.uast.test.kotlin

import com.intellij.psi.*
import com.intellij.testFramework.RunAll
import com.intellij.testFramework.UsefulTestCase
import com.intellij.util.ThrowableRunnable
import org.jetbrains.kotlin.asJava.toLightAnnotation
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.kotlin.utils.addToStdlib.assertedCast
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.kotlin.utils.sure
import org.jetbrains.uast.*
import org.jetbrains.uast.expressions.UInjectionHost
import org.jetbrains.uast.kotlin.KotlinUastLanguagePlugin
import org.jetbrains.uast.test.env.kotlin.findElementByText
import org.jetbrains.uast.test.env.kotlin.findElementByTextFromPsi
import org.jetbrains.uast.visitor.AbstractUastVisitor
import org.junit.Assert
import org.junit.Test
import kotlin.test.fail as kfail


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

    @Test
    fun testSAM() {
        doTest("SAM") { _, file ->
            runAll(
                { assertNull(file.findElementByText<ULambdaExpression>("{ /* Not SAM */ }").functionalInterfaceType) }, {
                    assertEquals(
                        "java.lang.Runnable",
                        file.findElementByText<ULambdaExpression>("{/* Variable */}").functionalInterfaceType?.canonicalText
                    )
                }, {
                    assertEquals(
                        "java.lang.Runnable",
                        file.findElementByText<ULambdaExpression>("{/* Assignment */}").functionalInterfaceType?.canonicalText
                    )
                }, {
                    assertEquals(
                        "java.lang.Runnable",
                        file.findElementByText<ULambdaExpression>("{/* Type Cast */}").functionalInterfaceType?.canonicalText
                    )
                }, {
                    assertEquals(
                        "java.lang.Runnable",
                        file.findElementByText<ULambdaExpression>("{/* Argument */}").functionalInterfaceType?.canonicalText
                    )
                }, {
                    assertEquals(
                        "java.lang.Runnable",
                        file.findElementByText<ULambdaExpression>("{/* Return */}").functionalInterfaceType?.canonicalText
                    )
                }, {
                    assertEquals(
                        "java.lang.Runnable",
                        file.findElementByText<ULambdaExpression>("{ /* SAM */ }").functionalInterfaceType?.canonicalText
                    )
                }, {
                    assertEquals(
                        "java.lang.Runnable",
                        file.findElementByText<ULambdaExpression>("{ println(\"hello1\") }").functionalInterfaceType?.canonicalText
                    )
                }, {
                    assertEquals(
                        "java.lang.Runnable",
                        file.findElementByText<ULambdaExpression>("{ println(\"hello2\") }").functionalInterfaceType?.canonicalText
                    )
                }, {
                    val call = file.findElementByText<UCallExpression>("Runnable { println(\"hello2\") }")
                    assertEquals(
                        "java.lang.Runnable",
                        (call.classReference?.resolve() as? PsiClass)?.qualifiedName
                    )
                }, {
                    assertEquals(
                        "java.util.function.Supplier<T>",
                        file.findElementByText<ULambdaExpression>("{ \"Supplier\" }").functionalInterfaceType?.canonicalText
                    )
                }, {
                    assertEquals(
                        "java.util.concurrent.Callable<V>",
                        file.findElementByText<ULambdaExpression>("{ \"Callable\" }").functionalInterfaceType?.canonicalText
                    )
                }
            )
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

            file.findElementByTextFromPsi<UInjectionHost>("\"abc\"").let { literalExpression ->
                val psi = literalExpression.psi!!
                Assert.assertTrue(psi is KtStringTemplateExpression)
                UsefulTestCase.assertInstanceOf(literalExpression.uastParent, USwitchClauseExpressionWithBody::class.java)
            }

            file.findElementByTextFromPsi<UInjectionHost>("\"def\"").let { literalExpression ->
                val psi = literalExpression.psi!!
                Assert.assertTrue(psi is KtStringTemplateExpression)
                UsefulTestCase.assertInstanceOf(literalExpression.uastParent, USwitchClauseExpressionWithBody::class.java)
            }

            file.findElementByTextFromPsi<UInjectionHost>("\"def1\"").let { literalExpression ->
                val psi = literalExpression.psi!!
                Assert.assertTrue(psi is KtStringTemplateExpression)
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
    fun testEnumCallIdentifier() {
        doTest("EnumValuesConstructors") { _, file ->
            val enumEntry = file.findElementByTextFromPsi<UElement>("(\"system\")")
            enumEntry.accept(object : AbstractUastVisitor() {
                override fun visitCallExpression(node: UCallExpression): Boolean {
                    val methodIdentifier = node.methodIdentifier
                    assertEquals("SYSTEM", methodIdentifier?.name)
                    return super.visitCallExpression(node)
                }
            })
        }
    }

    @Test
    fun testEnumCallWithBodyIdentifier() {
        doTest("EnumValueMembers") { _, file ->
            val enumEntry = file.findElementByTextFromPsi<UElement>("(\"foo\")")
            enumEntry.accept(object : AbstractUastVisitor() {
                override fun visitCallExpression(node: UCallExpression): Boolean {
                    val methodIdentifier = node.methodIdentifier
                    assertEquals("SHEET", methodIdentifier?.name)
                    return super.visitCallExpression(node)
                }
            })
        }
    }

    @Test
    fun testSimpleAnnotated() {
        doTest("SimpleAnnotated") { _, file ->
            file.findElementByTextFromPsi<UField>("@kotlin.SinceKotlin(\"1.0\")\n    val property: String = \"Mary\"").let { field ->
                val annotation = field.annotations.assertedFind("kotlin.SinceKotlin") { it.qualifiedName }
                Assert.assertEquals("1.0", annotation.findDeclaredAttributeValue("version")?.evaluateString())
                Assert.assertEquals("SinceKotlin", annotation.cast<UAnchorOwner>().uastAnchor?.sourcePsi?.text)
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
        file.findElementByTextFromPsi<UElement>("@AnnotationArray(value = Annotation(\"sv1\", \"sv2\"))")
            .findElementByTextFromPsi<UElement>("Annotation(\"sv1\", \"sv2\")")
            .sourcePsiElement
            .let { referenceExpression ->
                val convertedUAnnotation = referenceExpression
                    .cast<KtReferenceExpression>()
                    .toUElementOfType<UAnnotation>()
                        ?: throw AssertionError("haven't got annotation from $referenceExpression(${referenceExpression?.javaClass})")

                checkDescriptorsLeak(convertedUAnnotation)
                assertEquals("Annotation", convertedUAnnotation.qualifiedName)
                val lightAnnotation = convertedUAnnotation.getAsJavaPsiElement(PsiAnnotation::class.java)
                        ?: throw AssertionError("can't get lightAnnotation from $convertedUAnnotation")
                assertEquals("Annotation", lightAnnotation.qualifiedName)
                assertEquals("Annotation", (convertedUAnnotation as UAnchorOwner).uastAnchor?.sourcePsi?.text)
            }
    }

    @Test
    fun testNestedAnnotationParameters() = doTest("AnnotationComplex") { _, file ->

        fun UFile.annotationAndParam(refText: String, check: (PsiAnnotation, String?) -> Unit) {
            findElementByTextFromPsi<UElement>(refText)
                .let { expression ->
                    val (annotation: PsiAnnotation, paramname: String?) =
                        getContainingAnnotationEntry(expression) ?: kfail("annotation not found for '$refText' ($expression)")
                    check(annotation, paramname)
                }
        }

        file.annotationAndParam("sv1") { annotation, paramname ->
            assertEquals("Annotation", annotation.qualifiedName)
            assertEquals(null, paramname)
        }
        file.annotationAndParam("sv2") { annotation, paramname ->
            assertEquals("Annotation", annotation.qualifiedName)
            assertEquals(null, paramname)
        }
        file.annotationAndParam("sar1") { annotation, paramname ->
            assertEquals("Annotation", annotation.qualifiedName)
            assertEquals("strings", paramname)
        }
        file.annotationAndParam("sar2") { annotation, paramname ->
            assertEquals("Annotation", annotation.qualifiedName)
            assertEquals("strings", paramname)
        }
        file.annotationAndParam("[sar]1") { annotation, paramname ->
            assertEquals("Annotation", annotation.qualifiedName)
            assertEquals("strings", paramname)
        }
        file.annotationAndParam("[sar]2") { annotation, paramname ->
            assertEquals("Annotation", annotation.qualifiedName)
            assertEquals("strings", paramname)
        }
    }


    @Test
    fun testParametersDisorder() = doTest("ParametersDisorder") { _, file ->

        fun assertArguments(argumentsInPositionalOrder: List<String?>?, refText: String) =
            file.findElementByTextFromPsi<UCallExpression>(refText).let { call ->
                if (call !is UCallExpressionEx) throw AssertionError("${call.javaClass} is not a UCallExpressionEx")
                Assert.assertEquals(
                    argumentsInPositionalOrder,
                    call.resolve()?.let { psiMethod ->
                        (0 until psiMethod.parameterList.parametersCount).map {
                            call.getArgumentForParameter(it)?.asRenderString()
                        }
                    }
                )
            }


        assertArguments(listOf("2", "2.2"), "global(b = 2.2F, a = 2)")
        assertArguments(listOf(null, "\"bbb\""), "withDefault(d = \"bbb\")")
        assertArguments(listOf("1.3", "3.4"), "atan2(1.3, 3.4)")
        assertArguments(null, "unresolvedMethod(\"param1\", \"param2\")")
        assertArguments(listOf("\"%i %i %i\"", "varargs 1 : 2 : 3"), "format(\"%i %i %i\", 1, 2, 3)")
        assertArguments(listOf("\"%i %i %i\"", "varargs arrayOf(1, 2, 3)"), "format(\"%i %i %i\", arrayOf(1, 2, 3))")
        assertArguments(
            listOf("\"%i %i %i\"", "varargs arrayOf(1, 2, 3) : arrayOf(4, 5, 6)"),
            "format(\"%i %i %i\", arrayOf(1, 2, 3), arrayOf(4, 5, 6))"
        )
        assertArguments(listOf("\"%i %i %i\"", "\"\".chunked(2).toTypedArray()"), "format(\"%i %i %i\", *\"\".chunked(2).toTypedArray())")
        assertArguments(listOf("\"def\"", "8", "7.0"), "with2Receivers(8, 7.0F)")
    }

    @Test
    fun testResolvedDeserializedMethod() = doTest("Resolve") { _, file ->

        fun UElement.assertResolveCall(callText: String, methodName: String = callText.substringBefore("(")) {
            this.findElementByTextFromPsi<UCallExpression>(callText).let {
                val resolve = it.resolve().sure { "resolving '$callText'" }
                assertEquals(methodName, resolve.name)
            }
        }

        file.findElementByTextFromPsi<UElement>("bar").getParentOfType<UMethod>()!!.let { barMethod ->
            barMethod.assertResolveCall("foo()")
            barMethod.assertResolveCall("inlineFoo()")
            barMethod.assertResolveCall("forEach { println(it) }", "forEach")
            barMethod.assertResolveCall("joinToString()")
            barMethod.assertResolveCall("last()")
            barMethod.assertResolveCall("setValue(\"123\")")
            barMethod.assertResolveCall("contains(2 as Int)", "longRangeContains")
            barMethod.assertResolveCall("IntRange(1, 2)")
        }

        file.findElementByTextFromPsi<UElement>("barT").getParentOfType<UMethod>()!!.let { barMethod ->
            barMethod.assertResolveCall("foo()")
        }

        file.findElementByTextFromPsi<UElement>("listT").getParentOfType<UMethod>()!!.let { barMethod ->
            barMethod.assertResolveCall("isEmpty()")
            barMethod.assertResolveCall("foo()")
        }

    }

    @Test
    fun testUtilsStreamLambda() {
        doTest("Lambdas") { _, file ->
            val lambda = file.findElementByTextFromPsi<ULambdaExpression>("{ it.isEmpty() }")
            assertEquals(
                "java.util.function.Predicate<? super java.lang.String>",
                lambda.functionalInterfaceType?.canonicalText
            )
            assertEquals(
                "kotlin.jvm.functions.Function1<? super java.lang.String,? extends java.lang.Boolean>",
                lambda.getExpressionType()?.canonicalText
            )
            val uCallExpression = lambda.uastParent.assertedCast<UCallExpression> { "UCallExpression expected" }
            assertTrue(uCallExpression.valueArguments.contains(lambda))
        }
    }

    @Test
    fun testLambdaParamCall() {
        doTest("Lambdas") { _, file ->
            val lambdaCall = file.findElementByTextFromPsi<UCallExpression>("selectItemFunction()")
            assertEquals(
                "UIdentifier (Identifier (selectItemFunction))",
                lambdaCall.methodIdentifier?.asLogString()
            )
            assertEquals(
                "selectItemFunction",
                lambdaCall.methodIdentifier?.name
            )
            assertEquals(
                "invoke",
                lambdaCall.methodName
            )
            val receiver = lambdaCall.receiver ?: kfail("receiver expected")
            assertEquals("UReferenceExpression", receiver.asLogString())
            val uParameter = (receiver as UReferenceExpression).resolve().toUElement() ?: kfail("uelement expected")
            assertEquals("UParameter (name = selectItemFunction)", uParameter.asLogString())
        }
    }

    @Test
    fun testLocalLambdaCall() {
        doTest("Lambdas") { _, file ->
            val lambdaCall = file.findElementByTextFromPsi<UCallExpression>("baz()")
            assertEquals(
                "UIdentifier (Identifier (baz))",
                lambdaCall.methodIdentifier?.asLogString()
            )
            assertEquals(
                "baz",
                lambdaCall.methodIdentifier?.name
            )
            assertEquals(
                "invoke",
                lambdaCall.methodName
            )
            val receiver = lambdaCall.receiver ?: kfail("receiver expected")
            assertEquals("UReferenceExpression", receiver.asLogString())
            val uParameter = (receiver as UReferenceExpression).resolve().toUElement() ?: kfail("uelement expected")
            assertEquals("ULocalVariable (name = baz)", uParameter.asLogString())
        }
    }

    @Test
    fun testLocalDeclarationCall() {
        doTest("LocalDeclarations") { _, file ->
            val localFunction = file.findElementByTextFromPsi<UElement>("bar() == Local()").
                findElementByText<UCallExpression>("bar()")
            assertEquals(
                "UIdentifier (Identifier (bar))",
                localFunction.methodIdentifier?.asLogString()
            )
            assertEquals(
                "bar",
                localFunction.methodIdentifier?.name
            )
            assertEquals(
                "bar",
                localFunction.methodName
            )
            val localFunctionResolved = localFunction.resolve()
            assertNotNull(localFunctionResolved)
            val receiver = localFunction.receiver ?: kfail("receiver expected")
            assertEquals("UReferenceExpression", receiver.asLogString())
            val uVariable = (receiver as UReferenceExpression).resolve().toUElement() ?: kfail("uelement expected")
            assertEquals("ULocalVariable (name = bar)", uVariable.asLogString())
            assertEquals((uVariable as ULocalVariable).uastInitializer, localFunctionResolved.toUElement())
        }
    }

    @Test
    fun testLocalConstructorCall() {
        doTest("LocalDeclarations") { _, file ->
            val localFunction = file.findElementByTextFromPsi<UElement>("bar() == Local()").
                findElementByText<UCallExpression>("Local()")
            assertEquals(
                "UIdentifier (Identifier (Local))",
                localFunction.methodIdentifier?.asLogString()
            )
            assertEquals(
                "Local",
                localFunction.methodIdentifier?.name
            )
            assertEquals(
                "<init>",
                localFunction.methodName
            )
            val localFunctionResolved = localFunction.resolve()
            assertNotNull(localFunctionResolved)
            val classReference = localFunction.classReference ?: kfail("classReference expected")
            assertEquals("USimpleNameReferenceExpression (identifier = <init>, resolvesTo = PsiClass: Local)", classReference.asLogString())
            val localClass = classReference.resolve().toUElement() ?: kfail("uelement expected")
            assertEquals("UClass (name = Local)", localClass.asLogString())
            val localPrimaryConstructor = localFunctionResolved.toUElementOfType<UMethod>() ?: kfail("constructor expected")
            assertTrue(localPrimaryConstructor.isConstructor)
            assertEquals(localClass.javaPsi, localPrimaryConstructor.javaPsi.cast<PsiMethod>().containingClass)
        }
    }

    @Test
    fun testMethodReturnTypeReference() {
        doTest("Elvis") { _, file ->
            assertEquals(
                "UTypeReferenceExpression (name = java.lang.String)",
                file.findElementByTextFromPsi<UMethod>("fun foo(bar: String): String? = null").returnTypeReference?.asLogString()
            )
            assertEquals(
                null,
                file.findElementByTextFromPsi<UMethod>("fun bar() = 42").returnTypeReference?.asLogString()
            )

        }
    }

    @Test
    fun testVariablesTypeReferences() {
        doTest("TypeReferences") { _, file ->
            run {
                val localVariable = file.findElementByTextFromPsi<UVariable>("val varWithType: String? = \"Not Null\"")
                val typeReference = localVariable.typeReference
                assertEquals("java.lang.String", typeReference?.getQualifiedName())
                val sourcePsi = typeReference?.sourcePsi ?: kfail("no sourcePsi")
                assertTrue("sourcePsi = $sourcePsi should be physical", sourcePsi.isPhysical)
                assertEquals("String?", sourcePsi.text)
            }

            run {
                val localVariable = file.findElementByTextFromPsi<UVariable>("val varWithoutType = \"lorem ipsum\"")
                val typeReference = localVariable.typeReference
                assertEquals("java.lang.String", typeReference?.getQualifiedName())
                assertNull(typeReference?.sourcePsi)
            }

            run {
                val localVariable = file.findElementByTextFromPsi<UVariable>("parameter: Int")
                val typeReference = localVariable.typeReference
                assertEquals("int", typeReference?.type?.presentableText)
                val sourcePsi = typeReference?.sourcePsi ?: kfail("no sourcePsi")
                assertTrue("sourcePsi = $sourcePsi should be physical", sourcePsi.isPhysical)
                assertEquals("Int", sourcePsi.text)
            }
        }
    }

    @Test
    fun testReifiedReturnTypes() {
        doTest("ReifiedReturnType") { _, file ->
            val methods = file.classes.flatMap { it.methods.asIterable() }
            assertEquals("""
                function1 -> PsiType:void
                function2 -> PsiType:T
                function2CharSequence -> PsiType:T extends PsiType:CharSequence
                copyWhenGreater -> PsiType:B extends PsiType:T extends PsiType:CharSequence, PsiType:Comparable<? super T>
                function3 -> PsiType:void
                function4 -> PsiType:T
                function5 -> PsiType:int
                function6 -> PsiType:T
                function7 -> PsiType:T
                function8 -> PsiType:T
                function9 -> PsiType:T
                function10 -> PsiType:T
                function11 -> PsiType:T
                function11CharSequence -> PsiType:T extends PsiType:CharSequence
                function12CharSequence -> PsiType:B extends PsiType:T extends PsiType:CharSequence
                Foo -> null
                foo -> PsiType:Z extends PsiType:T
            """.trimIndent(), methods.joinToString("\n") { m ->
                buildString {
                    append(m.name).append(" -> ")
                    fun PsiType.typeWithExtends(): String = buildString {
                        append(this@typeWithExtends)
                        this@typeWithExtends.safeAs<PsiClassType>()?.resolve()?.extendsList?.referencedTypes?.takeIf { it.isNotEmpty() }
                            ?.let { e ->
                                append(" extends ")
                                append(e.joinToString(", ") { it.typeWithExtends() })
                            }
                    }
                    append(m.returnType?.typeWithExtends())
                }
            })
            for (method in methods.drop(3)) {
                assertEquals("assert return types comparable for '${method.name}'", method.returnType, method.returnType)
            }
        }
    }

    @Test
    fun testReifiedParameters() {
        doTest("ReifiedParameters") { _, file ->
            val methods = file.classes.flatMap { it.methods.asIterable() }

            for (method in methods) {
                assertNotNull("method ${method.name} should have source", method.sourcePsi)
                assertEquals("method ${method.name} should be equals to converted from sourcePsi", method, method.sourcePsi.toUElement())
                assertEquals("method ${method.name} should be equals to converted from javaPsi", method, method.javaPsi.toUElement())

                for (parameter in method.uastParameters) {
                    assertNotNull("parameter ${parameter.name} should have source", parameter.sourcePsi)
                    assertEquals(
                        "parameter ${parameter.name} of method ${method.name} should be equals to converted from sourcePsi",
                        parameter,
                        parameter.sourcePsi.toUElementOfType<UParameter>()
                    )
                    assertEquals(
                        "parameter ${parameter.name} of method ${method.name} should be equals to converted from javaPsi",
                        parameter,
                        parameter.javaPsi.toUElement()
                    )
                }
            }
        }
    }

}

fun <T, R> Iterable<T>.assertedFind(value: R, transform: (T) -> R): T =
    find { transform(it) == value } ?: throw AssertionError("'$value' not found, only ${this.joinToString { transform(it).toString() }}")

fun runAll(vararg asserts: () -> Unit) = RunAll(*asserts.map { ThrowableRunnable<Throwable>(it) }.toTypedArray()).run()