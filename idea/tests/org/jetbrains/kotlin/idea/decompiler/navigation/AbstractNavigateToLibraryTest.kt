/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.decompiler.navigation

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.navigation.NavigationTestUtils
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.test.*
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import java.util.*

abstract class AbstractNavigateToLibraryTest : KotlinCodeInsightTestCase() {

    protected fun doTest(path: String): Unit = doTestEx(path)

    protected fun doWithJSModuleTest(path: String): Unit = doTestEx(path) {
        val jsModule = this.createModule("js-module")
        jsModule.configureAs(ModuleKind.KOTLIN_JAVASCRIPT)
    }

    abstract val withSource: Boolean
    abstract val expectedFileExt: String

    protected fun doTestEx(path: String, additionalConfig: (() -> Unit)? = null) {
        configureByFile(path)
        module.configureAs(getProjectDescriptor())

        if (additionalConfig != null) {
            additionalConfig()
        }

        checkAnnotatedLibraryCode(path, false)
        checkAnnotatedLibraryCode(path, true)
    }

    override fun tearDown() {
        SourceNavigationHelper.setForceResolve(false)
        super.tearDown()
    }

    override fun getTestDataPath(): String =
            KotlinTestUtils.getHomeDirectory() + File.separator

    private fun checkAnnotatedLibraryCode(path: String, forceResolve: Boolean) {
        SourceNavigationHelper.setForceResolve(forceResolve)
        val actualCode = NavigationTestUtils.getNavigateElementsText(project, collectInterestingNavigationElements())
        KotlinTestUtils.assertEqualsToFile(File(path.replace(".kt", expectedFileExt)), actualCode)
    }

    private fun collectInterestingReferences(): Collection<KtReference> {
        val psiFile = file
        val referenceContainersToReferences = LinkedHashMap<PsiElement, KtReference>()
        for (offset in 0..psiFile.textLength - 1) {
            val ref = psiFile.findReferenceAt(offset)
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

        val targetNavPsiFile = target.navigationElement.containingFile ?: return

        val targetNavFile = targetNavPsiFile.virtualFile ?: return

        if (!ProjectRootsUtil.isProjectSourceFile(project, targetNavFile)) {
            put(ref.element, ref)
        }
    }

    private fun collectInterestingNavigationElements() =
            collectInterestingReferences().map {
                val target = it.resolve()
                TestCase.assertNotNull(target)
                target!!.navigationElement
            }

    open fun getProjectDescriptor(): KotlinLightProjectDescriptor =
            JdkAndMockLibraryProjectDescriptor(PluginTestCaseBase.getTestDataPathBase() + "/decompiler/navigation/library", withSource)
}

abstract class AbstractNavigateToDecompiledLibraryTest : AbstractNavigateToLibraryTest() {
    override val withSource: Boolean get() = false
    override val expectedFileExt: String get() = ".decompiled.expected"
}

abstract class AbstractNavigateToLibrarySourceTest : AbstractNavigateToLibraryTest() {
    override val withSource: Boolean get() = true
    override val expectedFileExt: String get() = ".source.expected"
}