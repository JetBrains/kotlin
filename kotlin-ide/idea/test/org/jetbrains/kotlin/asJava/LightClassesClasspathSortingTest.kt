/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.impl.ResolveScopeManager
import com.intellij.util.ThrowableRunnable
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.idea.caches.lightClasses.KtLightClassForDecompiledDeclaration
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.MockLibraryFacility
import org.jetbrains.kotlin.idea.test.runAll
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.test.TestRoot
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertNotNull

@TestRoot("idea")
@TestMetadata("testData/decompiler/lightClassesOrder")
@RunWith(JUnit38ClassRunner::class)
class LightClassesClasspathSortingTest : KotlinLightCodeInsightFixtureTestCase() {
    private val mockLibraryFacility = MockLibraryFacility(File(testDataPath, getTestName(true)))

    fun testExplicitClass() {
        doTest("test1.A")
    }

    fun testFileClass() {
        doTest("test2.FileKt")
    }

    override fun setUp() {
        super.setUp()
        mockLibraryFacility.setUp(module)
    }

    override fun tearDown() {
        runAll(
            ThrowableRunnable { mockLibraryFacility.tearDown(module) },
            ThrowableRunnable { super.tearDown() }
        )
    }

    private fun doTest(fqName: String) {
        // Same classes are in sources and in compiled Kotlin library. Test that light classes from sources have a priority.

        val dirName = getTestName(true)

        val testDirRoot = File(testDataPath)
        val filePaths = File(testDirRoot, dirName).listFiles().orEmpty().map { it.toRelativeString(testDirRoot) }.toTypedArray()
        myFixture.configureByFiles(*filePaths)

        checkLightClassBeforeDecompiled(fqName)
    }

    private fun checkLightClassBeforeDecompiled(fqName: String) {
        val psiClass = JavaPsiFacade.getInstance(project).findClass(fqName, ResolveScopeManager.getElementResolveScope(file))

        assertNotNull(psiClass, "Can't find class for $fqName")
        assert(psiClass is KtLightClassForSourceDeclaration || psiClass is KtLightClassForFacade) { "Should be an explicit light class, but was $fqName ${psiClass::class.java}" }
        assert(psiClass !is KtLightClassForDecompiledDeclaration) { "Should not be decompiled light class: $fqName ${psiClass::class.java}" }
    }
}