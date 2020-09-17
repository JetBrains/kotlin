package org.jetbrains.uast.test.kotlin

import org.jetbrains.uast.UFile
import org.junit.Ignore
import org.junit.Test
import java.io.File

class KotlinIDERenderLogTest : AbstractKotlinUastLightCodeInsightFixtureTest(), AbstractKotlinRenderLogTest {

    override fun getTestFile(testName: String, ext: String): File {
        if (ext.endsWith(".txt")) {
            val testFile = super.getTestFile(testName, ext.removeSuffix(".txt") + "-ide.txt")
            if (testFile.exists()) return testFile
        }
        return super.getTestFile(testName, ext)
    }

    override fun check(testName: String, file: UFile) = super.check(testName, file)

    @Test
    fun testLocalDeclarations() = doTest("LocalDeclarations")

    @Test
    fun testSimple() = doTest("Simple")

    @Test
    fun testWhenIs() = doTest("WhenIs")

    @Test
    fun testDefaultImpls() = doTest("DefaultImpls")

    @Test
    fun testBitwise() = doTest("Bitwise")

    @Test
    fun testElvis() = doTest("Elvis")

    @Test
    fun testPropertyAccessors() = doTest("PropertyAccessors")

    @Test
    fun testPropertyInitializer() = doTest("PropertyInitializer")

    @Test
    fun testPropertyInitializerWithoutSetter() = doTest("PropertyInitializerWithoutSetter")

    @Test
    fun testAnnotationParameters() = doTest("AnnotationParameters")

    @Test
    fun testEnumValueMembers() = doTest("EnumValueMembers")

    @Test
    fun testEnumValuesConstructors() = doTest("EnumValuesConstructors")

    @Test
    fun testStringTemplate() = doTest("StringTemplate")

    @Test
    fun testStringTemplateComplex() = doTest("StringTemplateComplex")

    @Test
    fun testStringTemplateComplexForUInjectionHost() = withForceUInjectionHostValue {
        doTest("StringTemplateComplexForUInjectionHost")
    }

    @Test
    fun testQualifiedConstructorCall() = doTest("QualifiedConstructorCall")

    @Test
    fun testPropertyDelegate() = doTest("PropertyDelegate")

    @Test
    fun testLocalVariableWithAnnotation() = doTest("LocalVariableWithAnnotation")

    @Test
    fun testPropertyWithAnnotation() = doTest("PropertyWithAnnotation")

    @Test
    fun testIfStatement() = doTest("IfStatement")

    @Test
    fun testInnerClasses() = doTest("InnerClasses")

    @Test
    @Ignore // there is a descriptor leak probably, unignore when fixed
    fun ingoretestSimpleScript() = doTest("SimpleScript") { testName, file -> check(testName, file, false) }

    @Test
    fun testDestructuringDeclaration() = doTest("DestructuringDeclaration")

    @Test
    fun testDefaultParameterValues() = doTest("DefaultParameterValues")

    @Test
    fun testParameterPropertyWithAnnotation() = doTest("ParameterPropertyWithAnnotation")

    @Test
    fun testParametersWithDefaultValues() = doTest("ParametersWithDefaultValues")

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
    fun testConstructorDelegate() = doTest("ConstructorDelegate") { testName, file ->
        // Igor Yakovlev told that delegation is a little bit broken in ULC and not expected to be fixed
        check(testName, file, false)
    }

    @Test
    fun testLambdaReturn() = doTest("LambdaReturn")

    @Test
    fun testReified() = doTest("Reified")

    @Test
    fun testReifiedReturnType() = doTest("ReifiedReturnType")

    @Test
    fun testReifiedParameters() = doTest("ReifiedParameters")

    @Test
    fun testSuspend() = doTest("Suspend")

    @Test
    fun testDeprecatedHidden() = doTest("DeprecatedHidden")

    @Test
    fun testTryCatch() = doTest("TryCatch")

    @Test
    fun testComments() = doTest("Comments")
}
