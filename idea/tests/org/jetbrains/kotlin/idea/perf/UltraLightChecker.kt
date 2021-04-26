/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.openapi.Disposable
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Conditions
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.SyntaxTraverser
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.UsefulTestCase
import com.intellij.util.PairProcessor
import com.intellij.util.ref.DebugReflectionUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.PsiClassRenderer
import org.jetbrains.kotlin.asJava.classes.*
import org.jetbrains.kotlin.asJava.renderClass
import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCaseBase
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Assert
import java.io.File
import kotlin.test.assertFails

fun UsefulTestCase.forceUsingOldLightClassesForTest() {
    KtUltraLightSupport.forceUsingOldLightClasses = true
    Disposer.register(testRootDisposable, Disposable {
        KtUltraLightSupport.forceUsingOldLightClasses = false
    })
}

object UltraLightChecker {
    fun checkClassEquivalence(file: KtFile) {
        for (ktClass in allClasses(file)) {
            checkClassEquivalence(ktClass)
        }
    }

    fun checkForReleaseCoroutine(sourceFileText: String, module: Module) {
        if (sourceFileText.contains("//RELEASE_COROUTINE_NEEDED")) {
            TestCase.assertTrue(
                "Test should be runned under language version that supports released coroutines",
                module.languageVersionSettings.supportsFeature(LanguageFeature.ReleaseCoroutines)
            )
        }
    }

    fun allClasses(file: KtFile): List<KtClassOrObject> =
        SyntaxTraverser.psiTraverser(file).filter(KtClassOrObject::class.java).toList()

    fun checkFacadeEquivalence(
        fqName: FqName,
        searchScope: GlobalSearchScope,
        project: Project
    ): KtLightClassForFacade? {

        val oldForceFlag = KtUltraLightSupport.forceUsingOldLightClasses
        KtUltraLightSupport.forceUsingOldLightClasses = true
        val gold = KtLightClassForFacadeImpl.createForFacadeNoCache(fqName, searchScope, project)
        KtUltraLightSupport.forceUsingOldLightClasses = false
        val ultraLightClass = KtLightClassForFacadeImpl.createForFacadeNoCache(fqName, searchScope, project) ?: return null
        KtUltraLightSupport.forceUsingOldLightClasses = oldForceFlag

        checkClassEquivalenceByRendering(gold, ultraLightClass)

        return ultraLightClass
    }


    fun checkByJavaFile(testDataPath: String, lightClasses: List<KtLightClass>) {
        val expectedTextFile = getJavaFileForTest(testDataPath)
        val renderedResult = renderLightClasses(testDataPath, lightClasses)
            KotlinTestUtils.assertEqualsToFile(expectedTextFile, renderedResult)
    }

    fun getJavaFileForTest(testDataPath: String): File {
        val expectedTextFile = KotlinTestUtils.replaceExtension(File(testDataPath), "java")
        KotlinLightCodeInsightFixtureTestCaseBase.assertTrue(expectedTextFile.exists())
        return expectedTextFile
    }

    fun renderLightClasses(testDataPath: String, lightClasses: List<KtLightClass>): String {
        val extendedTypeRendererOld = PsiClassRenderer.extendedTypeRenderer
        return try {
            PsiClassRenderer.extendedTypeRenderer = testDataPath.endsWith("typeAnnotations.kt")
            lightClasses.joinToString("\n\n") { it.renderClass() }
        } finally {
            PsiClassRenderer.extendedTypeRenderer = extendedTypeRendererOld
        }
    }

    fun checkClassEquivalence(ktClass: KtClassOrObject): KtUltraLightClass? {
        val gold = KtLightClassForSourceDeclaration.createNoCache(
            ktClass, ktClass.languageVersionSettings.getFlag(JvmAnalysisFlags.jvmDefaultMode), forceUsingOldLightClasses = true
        )
        val ultraLightClass = LightClassGenerationSupport.getInstance(ktClass.project).createUltraLightClass(ktClass) ?: return null

        val secondULInstance = LightClassGenerationSupport.getInstance(ktClass.project).createUltraLightClass(ktClass)
        Assert.assertNotNull(secondULInstance)
        Assert.assertTrue(ultraLightClass !== secondULInstance)
        secondULInstance!!
        Assert.assertEquals(ultraLightClass.ownMethods.size, secondULInstance.ownMethods.size)
        Assert.assertTrue(ultraLightClass.ownMethods.containsAll(secondULInstance.ownMethods))

        checkClassEquivalenceByRendering(gold, ultraLightClass)

        return ultraLightClass
    }

    fun checkScriptEquivalence(script: KtScript): KtLightClass {

        val ultraLightScript: KtLightClass?

        val oldForceFlag = KtUltraLightSupport.forceUsingOldLightClasses
        try {
            KtUltraLightSupport.forceUsingOldLightClasses = false
            ultraLightScript = KotlinAsJavaSupport.getInstance(script.project).getLightClassForScript(script)
            TestCase.assertTrue(ultraLightScript is KtUltraLightClassForScript)
            ultraLightScript!!
            val gold = KtLightClassForScript.createNoCache(script, forceUsingOldLightClasses = true)
            checkClassEquivalenceByRendering(gold, ultraLightScript)
        } finally {
            KtUltraLightSupport.forceUsingOldLightClasses = oldForceFlag
        }

        return ultraLightScript!!
    }

    private fun checkClassEquivalenceByRendering(gold: PsiClass?, ultraLightClass: PsiClass) {
        if (gold != null) {
            Assert.assertFalse(gold.javaClass.name.contains("Ultra"))
        }

        val goldText = gold?.renderClass().orEmpty()
        val ultraText = ultraLightClass.renderClass()

        if (goldText != ultraText) {
            Assert.assertEquals(
                "// Classic implementation:\n$goldText",
                "// Ultra-light implementation:\n$ultraText"
            )
        }
    }

    private fun checkDescriptorLeakOnElement(element: PsiElement) {
        DebugReflectionUtil.walkObjects(
            10,
            mapOf(element to element.javaClass.name),
            Any::class.java,
            Conditions.alwaysTrue(),
            PairProcessor { value, backLink ->
                if (value is DeclarationDescriptor) {
                    assertFails {
                        """Leaked descriptor ${value.javaClass.name} in ${element.javaClass.name}\n$backLink"""
                    }
                }
                true
            })
    }

    fun checkDescriptorsLeak(lightClass: KtLightClass) {
        checkDescriptorLeakOnElement(lightClass)
        lightClass.methods.forEach {
            checkDescriptorLeakOnElement(it)
            it.parameterList.parameters.forEach { parameter -> checkDescriptorLeakOnElement(parameter) }
        }
        lightClass.fields.forEach { checkDescriptorLeakOnElement(it) }
    }
}
