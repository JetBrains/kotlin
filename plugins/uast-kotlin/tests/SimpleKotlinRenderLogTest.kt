package org.jetbrains.uast.test.kotlin

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

    @Test fun testQualifiedConstructorCall() = doTest("QualifiedConstructorCall")

    @Test fun testPropertyDelegate() = doTest("PropertyDelegate")

    @Test fun testPropertyWithAnnotation() = doTest("PropertyWithAnnotation")

    @Test fun testInnerClasses() = doTest("InnerClasses")

    @Test fun testSimpleScript() = doTest("SimpleScript")

    @Test fun testParameterPropertyWithAnnotation() = doTest("ParameterPropertyWithAnnotation")

    @Test fun testParametersWithDefaultValues() = doTest("ParametersWithDefaultValues")
}