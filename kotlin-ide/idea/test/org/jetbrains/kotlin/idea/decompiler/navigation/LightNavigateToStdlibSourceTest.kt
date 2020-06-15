/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.decompiler.navigation

import com.intellij.psi.PsiElement
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.test.*
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.RunWith

@RunWith(JUnit38ClassRunner::class)
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
    fun testNavigateToJVMActualClassDeclarations() {
        val checkedTypes = listOf(
            // abstract actual classes in JVM:
            "AbstractMutableCollection<String>",
            "AbstractMutableList<String>",
            "AbstractMutableSet<String>",
            "AbstractMutableMap<String, String>",

            // actual typealiases in JVM:
            "ArrayList<String>",
            "HashSet<String>"
        )

        for (type in checkedTypes) {
            doTest("fun some() { val collection: <caret>$type? = null }") { navigationElement ->
                TestCase.assertTrue(
                    "$navigationElement is not instance of ${KtClassOrObject::class} or ${KtTypeAlias::class}",
                    navigationElement is KtClassOrObject || navigationElement is KtTypeAlias
                )

                TestCase.assertTrue(
                    "$navigationElement is not actual declaration",
                    (navigationElement as KtModifierListOwner).hasActualModifier()
                )
            }
        }
    }

    @ProjectDescriptorKind(KOTLIN_JVM_WITH_STDLIB_SOURCES)
    fun testRefToPrintlnWithJVM() {
        doTest(
            "fun foo() { <caret>println() }",
            "Console.kt"
        )
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
            "Console.kt"
        )
    }

    @ProjectDescriptorKind(KOTLIN_JAVASCRIPT_WITH_ADDITIONAL_JVM_WITH_STDLIB)
    fun testRefToPrintlnWithJSAndJVM() {
        doTest(
            "fun foo() { <caret>println() }",
            "console.kt"
        )
    }

    override fun getProjectDescriptor() = getProjectDescriptorFromAnnotation()

    private fun doTest(text: String, sourceFileName: String) = doTest(text) { navigationElement ->
        // by default check by source file name
        TestCase.assertEquals(sourceFileName, navigationElement.containingFile.name)
    }

    private fun doTest(text: String, checker: (PsiElement) -> Unit) {
        myFixture.configureByText(KotlinFileType.INSTANCE, text)

        val ref = file.findReferenceAt(editor.caretModel.offset)
        val resolve = ref!!.resolve()
        val navigationElement = resolve!!.navigationElement

        checker(navigationElement)
    }
}

