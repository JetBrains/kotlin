/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.decompiler.navigation

import junit.framework.TestCase
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.test.*

class LightNavigateToStdlibSourceTest : KotlinLightCodeInsightFixtureTestCase() {
    @ProjectDescriptorKind(JDK_AND_MULTIPLATFORM_STDLIB_WITH_SOURCES)
    fun testNavigateToCommonDeclarationWhenPlatformSpecificOverloadAvailable() {
        doTest(
            "fun some() { <caret>mapOf(1 to 2, 3 to 4) }",
            "Maps.kt"
        )
    }

    @ProjectDescriptorKind(JDK_AND_MULTIPLATFORM_STDLIB_WITH_SOURCES)
    fun testNavigateToJVMSpecificationWithoutExpectActual() {
        doTest(
            "fun some() { <caret>mapOf(1 to 2) }",
            "MapsJVM.kt"
        )
    }

    @ProjectDescriptorKind(KOTLIN_JVM_WITH_STDLIB_SOURCES)
    fun testRefToPrintlnWithJVM() {
        doTest(
            "fun foo() { <caret>println() }",
            "Console.kt")
    }

    @ProjectDescriptorKind(KOTLIN_JAVASCRIPT)
    fun testRefToPrintlnWithJS() {
        doTest(
            "fun foo() { <caret>println() }",
            "console.kt"
        )
    }

    @ProjectDescriptorKind(KOTLIN_JVM_WITH_STDLIB_SOURCES_WITH_ADDITIONAL_JS)
    fun testRefToPrintlnWithJVMAndJS() {
        doTest(
            "fun foo() { <caret>println() }",
            "Console.kt")
    }

    @ProjectDescriptorKind(KOTLIN_JAVASCRIPT_WITH_ADDITIONAL_JVM_WITH_STDLIB)
    fun testRefToPrintlnWithJSAndJVM() {
        doTest(
            "fun foo() { <caret>println() }",
            "console.kt")
    }

    override fun getProjectDescriptor() = getProjectDescriptorFromAnnotation()

    private fun doTest(text: String, sourceFileName: String) {
        myFixture.configureByText(KotlinFileType.INSTANCE, text)

        val ref = file.findReferenceAt(editor.caretModel.offset)
        val resolve = ref!!.resolve()
        val navigationElement = resolve!!.navigationElement

        TestCase.assertEquals(sourceFileName, navigationElement.containingFile.name)
    }
}