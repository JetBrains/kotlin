/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.decompiler.navigation

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.navigation.NavigationTestUtils
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.test.*
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import java.util.*

abstract class AbstractNavigateToLibraryTest : KotlinLightCodeInsightFixtureTestCase() {
    abstract val expectedFileExt: String

    protected fun doTest(path: String) {
        myFixture.configureByFile(path)
        NavigationChecker.checkAnnotatedCode(file, File(path.replace(".kt", expectedFileExt)))
    }

    override fun tearDown() {
        SourceNavigationHelper.setForceResolve(false)
        super.tearDown()
    }

    override fun getTestDataPath(): String =
        KotlinTestUtils.getHomeDirectory() + File.separator
}

abstract class AbstractNavigateToDecompiledLibraryTest : AbstractNavigateToLibraryTest() {
    override val expectedFileExt: String get() = ".decompiled.expected"

    override fun getProjectDescriptor(): KotlinLightProjectDescriptor = PROJECT_DESCRIPTOR

    companion object {
        private val PROJECT_DESCRIPTOR = SdkAndMockLibraryProjectDescriptor(
            PluginTestCaseBase.getTestDataPathBase() + "/decompiler/navigation/library", false
        )
    }
}

abstract class AbstractNavigateToLibrarySourceTest : AbstractNavigateToLibraryTest() {
    override val expectedFileExt: String get() = ".source.expected"

    override fun getProjectDescriptor(): KotlinLightProjectDescriptor = PROJECT_DESCRIPTOR

    protected companion object {
        val PROJECT_DESCRIPTOR = SdkAndMockLibraryProjectDescriptor(
            PluginTestCaseBase.getTestDataPathBase() + "/decompiler/navigation/library", true
        )
    }
}

abstract class AbstractNavigateToLibrarySourceTestWithJS : AbstractNavigateToLibrarySourceTest() {
    override fun getProjectDescriptor(): KotlinLightProjectDescriptor = KotlinMultiModuleProjectDescriptor(
        "AbstractNavigateToLibrarySourceTestWithJS",
        AbstractNavigateToLibrarySourceTest.PROJECT_DESCRIPTOR,
        KotlinStdJSProjectDescriptor
    )
}

class NavigationChecker(val file: PsiFile, val referenceTargetChecker: (PsiElement) -> Unit) {
    fun annotatedLibraryCode(): String {
        return NavigationTestUtils.getNavigateElementsText(file.project, collectInterestingNavigationElements())
    }

    private fun collectInterestingNavigationElements() =
            collectInterestingReferences().map {
                val target = it.resolve()
                TestCase.assertNotNull(target)
                target!!.navigationElement
            }

    private fun collectInterestingReferences(): Collection<KtReference> {
        val referenceContainersToReferences = LinkedHashMap<PsiElement, KtReference>()
        for (offset in 0..file.textLength - 1) {
            val ref = file.findReferenceAt(offset)
            val refs = when (ref) {
                is KtReference -> listOf(ref)
                is PsiMultiReference -> ref.references.filterIsInstance<KtReference>()
                else -> emptyList<KtReference>()
            }

            refs.forEach { referenceContainersToReferences.addReference(it) }
        }
        return referenceContainersToReferences.values
    }

    private fun MutableMap<PsiElement, KtReference>.addReference(ref: KtReference) {
        if (containsKey(ref.element)) return
        val target = ref.resolve() ?: return

        referenceTargetChecker(target)

        val targetNavPsiFile = target.navigationElement.containingFile ?: return

        val targetNavFile = targetNavPsiFile.virtualFile ?: return

        if (!ProjectRootsUtil.isProjectSourceFile(target.project, targetNavFile)) {
            put(ref.element, ref)
        }
    }

    companion object {
        fun checkAnnotatedCode(file: PsiFile, expectedFile: File, referenceTargetChecker: (PsiElement) -> Unit = {}) {
            val navigationChecker = NavigationChecker(file, referenceTargetChecker)
            for (forceResolve in listOf(false, true)) {
                SourceNavigationHelper.setForceResolve(forceResolve)
                KotlinTestUtils.assertEqualsToFile(expectedFile, navigationChecker.annotatedLibraryCode())
            }
        }
    }
}