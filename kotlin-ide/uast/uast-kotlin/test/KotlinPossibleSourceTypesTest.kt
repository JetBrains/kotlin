package org.jetbrains.uast.test.kotlin

import org.jetbrains.uast.*
import org.jetbrains.uast.test.common.PossibleSourceTypesTestBase
import org.jetbrains.uast.test.common.allUElementSubtypes
import org.junit.Test


class KotlinPossibleSourceTypesTest : AbstractKotlinUastLightCodeInsightFixtureTest(), PossibleSourceTypesTestBase {

    override fun check(testName: String, file: UFile) {
        val psiFile = file.sourcePsi
        for (uastType in allUElementSubtypes) {
            checkConsistencyWithRequiredTypes(psiFile, uastType)
        }
        checkConsistencyWithRequiredTypes(psiFile, UClass::class.java, UMethod::class.java, UField::class.java)
        checkConsistencyWithRequiredTypes(
            psiFile,
            USimpleNameReferenceExpression::class.java,
            UQualifiedReferenceExpression::class.java,
            UCallableReferenceExpression::class.java
        )
    }

    @Test
    fun testAnnotationComplex() = doTest("AnnotationComplex")

    @Test
    fun testAnnotationParameters() = doTest("AnnotationParameters")

    @Test
    fun testAnonymous() = doTest("Anonymous")

    @Test
    fun testBitwise() = doTest("Bitwise")

    @Test
    fun testClassAnnotation() = doTest("ClassAnnotation")

    @Test
    fun testConstructors() = doTest("Constructors")

    @Test
    fun testConstructorDelegate() = doTest("ConstructorDelegate")

    @Test
    fun testDefaultImpls() = doTest("DefaultImpls")

    @Test
    fun testDefaultParameterValues() = doTest("DefaultParameterValues")

    @Test
    fun testDelegate() = doTest("Delegate")

    @Test
    fun testDeprecatedHidden() = doTest("DeprecatedHidden")

    @Test
    fun testDestructuringDeclaration() = doTest("DestructuringDeclaration")

    @Test
    fun testElvis() = doTest("Elvis")

    @Test
    fun testEnumValueMembers() = doTest("EnumValueMembers")

    @Test
    fun testEnumValuesConstructors() = doTest("EnumValuesConstructors")

    @Test
    fun testIfStatement() = doTest("IfStatement")

    @Test
    fun testInnerClasses() = doTest("InnerClasses")

    @Test
    fun testLambdaReturn() = doTest("LambdaReturn")

    @Test
    fun testLambdas() = doTest("Lambdas")

    @Test
    fun testLocalDeclarations() = doTest("LocalDeclarations")

    @Test
    fun testLocalVariableWithAnnotation() = doTest("LocalVariableWithAnnotation")

    @Test
    fun testParameterPropertyWithAnnotation() = doTest("ParameterPropertyWithAnnotation")

    @Test
    fun testParametersWithDefaultValues() = doTest("ParametersWithDefaultValues")

    @Test
    fun testParametersDisorder() = doTest("ParametersDisorder")

    @Test
    fun testPropertyAccessors() = doTest("PropertyAccessors")

    @Test
    fun testPropertyDelegate() = doTest("PropertyDelegate")

    @Test
    fun testPropertyInitializer() = doTest("PropertyInitializer")

    @Test
    fun testPropertyInitializerWithoutSetter() = doTest("PropertyInitializerWithoutSetter")

    @Test
    fun testPropertyWithAnnotation() = doTest("PropertyWithAnnotation")

    @Test
    fun testReceiverFun() = doTest("ReceiverFun")

    @Test
    fun testReified() = doTest("Reified")

    @Test
    fun testReifiedParameters() = doTest("ReifiedParameters")

    @Test
    fun testReifiedReturnType() = doTest("ReifiedReturnType")

    @Test
    fun testQualifiedConstructorCall() = doTest("QualifiedConstructorCall")

    @Test
    fun testSimple() = doTest("Simple")

    @Test
    fun testStringTemplate() = doTest("StringTemplate")

    @Test
    fun testStringTemplateComplex() = doTest("StringTemplateComplex")

    @Test
    fun testStringTemplateComplexForUInjectionHost() = doTest("StringTemplateComplexForUInjectionHost")

    @Test
    fun testSuperCalls() = doTest("SuperCalls")

    @Test
    fun testSuspend() = doTest("Suspend")

    @Test
    fun testTryCatch() = doTest("TryCatch")

    @Test
    fun testTypeReferences() = doTest("TypeReferences")

    @Test
    fun testUnexpectedContainer() = doTest("UnexpectedContainerException")

    @Test
    fun testWhenAndDestructing() = doTest("WhenAndDestructing")

    @Test
    fun testWhenIs() = doTest("WhenIs")

    @Test
    fun testWhenStringLiteral() = doTest("WhenStringLiteral")
}