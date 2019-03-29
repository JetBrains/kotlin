/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.evaluate

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.completion.test.AbstractJvmBasicCompletionTest
import org.jetbrains.kotlin.idea.completion.test.testCompletion
import org.jetbrains.kotlin.idea.test.SdkAndMockLibraryProjectDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.DefaultBuiltInPlatforms
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

private val LIBRARY_SRC_PATH = KotlinTestUtils.getHomeDirectory() + "/idea/idea-completion/testData/codeFragmentInLibrarySource/customLibrary/"

class CodeFragmentCompletionInLibraryTest : AbstractJvmBasicCompletionTest() {

    override fun getProjectDescriptor() = object: SdkAndMockLibraryProjectDescriptor(LIBRARY_SRC_PATH, false) {
        override fun configureModule(module: Module, model: ModifiableRootModel) {
            super.configureModule(module, model)

            val library = model.moduleLibraryTable.getLibraryByName(SdkAndMockLibraryProjectDescriptor.LIBRARY_NAME)!!
            val modifiableModel = library.modifiableModel

            modifiableModel.addRoot(findLibrarySourceDir(), OrderRootType.SOURCES)
            modifiableModel.commit()
        }
    }

    fun testCompletionInCustomLibrary() {
        testCompletionInLibraryCodeFragment("<caret>", "EXIST: parameter")
    }

    fun testSecondCompletionInCustomLibrary() {
        testCompletionInLibraryCodeFragment("Ch<caret>", "EXIST: CharRange", "EXIST: Char", "INVOCATION_COUNT: 2")
    }

    fun testExtensionCompletionInCustomLibrary() {
        testCompletionInLibraryCodeFragment("3.extOn<caret>", "EXIST: extOnInt")
    }

    fun testJavaTypesCompletion() {
        testCompletionInLibraryCodeFragment("Hash<caret>", "EXIST: HashMap", "EXIST: HashSet")
    }

    private fun testCompletionInLibraryCodeFragment(fragmentText: String, vararg completionDirectives: String) {
        setupFixtureByCodeFragment(fragmentText)
        val directives = completionDirectives.map { "//$it" }.joinToString(separator = "\n")
        testCompletion(directives,
                       DefaultBuiltInPlatforms.jvmPlatform, { completionType, count -> myFixture.complete(completionType, count) })
    }

    private fun setupFixtureByCodeFragment(fragmentText: String) {
        val sourceFile = findLibrarySourceDir().findChild("customLibrary.kt")!!
        val jetFile = PsiManager.getInstance(project).findFile(sourceFile) as KtFile
        val fooFunctionFromLibrary = jetFile.declarations.first() as KtFunction
        val codeFragment = KtPsiFactory(fooFunctionFromLibrary).createExpressionCodeFragment(
                fragmentText,
                KotlinCodeFragmentFactory.getContextElement(fooFunctionFromLibrary.bodyExpression)
        )
        codeFragment.forceResolveScope(GlobalSearchScope.allScope(project))
        myFixture.configureFromExistingVirtualFile(codeFragment.virtualFile)
    }

    private fun findLibrarySourceDir(): VirtualFile {
        return LocalFileSystem.getInstance().findFileByIoFile(File(LIBRARY_SRC_PATH))!!
    }
}
