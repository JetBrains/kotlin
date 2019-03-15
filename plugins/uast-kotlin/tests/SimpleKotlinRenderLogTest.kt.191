package org.jetbrains.uast.test.kotlin

import org.jetbrains.uast.kotlin.KotlinConverter
import org.junit.Test

class SimpleKotlinRenderLogTest : AbstractKotlinRenderLogTest() {
    @Test fun testLocalDeclarations() = doTest("LocalDeclarations")

    @Test fun testSimple() = doTest("Simple")

    @Test fun testWhenIs() = doTest("WhenIs")

    @Test fun testDefaultImpls() = doTest("DefaultImpls")

    @Test fun testElvis() = doTest("Elvis")

    @Test fun testPropertyAccessors() = doTest("PropertyAccessors")

    @Test fun testPropertyInitializer() = doTest("PropertyInitializer")

    @Test fun testPropertyInitializerWithoutSetter() = doTest("PropertyInitializerWithoutSetter")

    @Test fun testAnnotationParameters() = doTest("AnnotationParameters")

    @Test fun testEnumValueMembers() = doTest("EnumValueMembers")

    @Test fun testStringTemplate() = doTest("StringTemplate")

    @Test fun testStringTemplateComplex() = doTest("StringTemplateComplex")

    @Test
    fun testStringTemplateComplexForUInjectionHost() = withForceUInjectionHostValue {
        doTest("StringTemplateComplexForUInjectionHost")
    }

    @Test fun testQualifiedConstructorCall() = doTest("QualifiedConstructorCall")

    @Test fun testPropertyDelegate() = doTest("PropertyDelegate")

    @Test fun testLocalVariableWithAnnotation() = doTest("LocalVariableWithAnnotation")

    @Test fun testPropertyWithAnnotation() = doTest("PropertyWithAnnotation")

    @Test fun testIfStatement() = doTest("IfStatement")

    @Test fun testInnerClasses() = doTest("InnerClasses")

    @Test fun testSimpleScript() = doTest("SimpleScript") { testName, file -> check(testName, file, false) }

    @Test fun testDestructuringDeclaration() = doTest("DestructuringDeclaration")

    @Test fun testDefaultParameterValues() = doTest("DefaultParameterValues")

    @Test fun testParameterPropertyWithAnnotation() = doTest("ParameterPropertyWithAnnotation")

    @Test fun testParametersWithDefaultValues() = doTest("ParametersWithDefaultValues")

    @Test
    fun testUnexpectedContainer() = doTest("UnexpectedContainerException")

    @Test
    fun testWhenStringLiteral() = doTest("WhenStringLiteral")

    @Test
    fun testWhenAndDestructing() = doTest("WhenAndDestructing") { testName, file -> check(testName, file, false) }

    @Test
    fun testSuperCalls() = doTest("SuperCalls")

    @Test
    fun testConstructors() = doTest("Constructors")

    @Test
    fun testClassAnnotation() = doTest("ClassAnnotation")

    @Test
    fun testReceiverFun() = doTest("ReceiverFun")

    @Test
    fun testAnonymous() = doTest("Anonymous")

    @Test
    fun testAnnotationComplex() = doTest("AnnotationComplex")

    @Test
    fun testParametersDisorder() = doTest("ParametersDisorder") { testName, file ->
        // disabled due to inconsistent parents for 2-receivers call (KT-22344)
        check(testName, file, false)
    }

    @Test
    fun testLambdas() = doTest("Lambdas")

    @Test
    fun testTypeReferences() = doTest("TypeReferences")

    @Test
    fun testDelegate() = doTest("Delegate")

    @Test
    fun testConstructorDelegate() = doTest("ConstructorDelegate")
}

fun withForceUInjectionHostValue(call: () -> Unit) {
    val prev = KotlinConverter.forceUInjectionHost
    KotlinConverter.forceUInjectionHost = true
    try {
        call.invoke()
    } finally {
        KotlinConverter.forceUInjectionHost = prev
    }
}