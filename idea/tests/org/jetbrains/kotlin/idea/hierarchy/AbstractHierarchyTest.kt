/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.hierarchy

import com.intellij.ide.hierarchy.*
import com.intellij.ide.hierarchy.actions.BrowseHierarchyActionBase
import com.intellij.ide.hierarchy.call.CallerMethodsTreeStructure
import com.intellij.ide.hierarchy.type.SubtypesHierarchyTreeStructure
import com.intellij.ide.hierarchy.type.SupertypesHierarchyTreeStructure
import com.intellij.ide.hierarchy.type.TypeHierarchyTreeStructure
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.fileEditor.impl.text.TextEditorPsiDataProvider
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.*
import com.intellij.refactoring.util.CommonRefactoringUtil.RefactoringErrorHintException
import com.intellij.rt.execution.junit.ComparisonDetailsExtractor
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.MapDataContext
import com.intellij.util.ArrayUtil
import junit.framework.ComparisonFailure
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.KotlinHierarchyViewTestBase
import org.jetbrains.kotlin.idea.hierarchy.calls.KotlinCalleeTreeStructure
import org.jetbrains.kotlin.idea.hierarchy.calls.KotlinCallerTreeStructure
import org.jetbrains.kotlin.idea.hierarchy.overrides.KotlinOverrideTreeStructure
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import java.util.*

/*
Test Hierarchy view
Format: test build hierarchy for element at caret, file with caret should be the first in the sorted list of files.
Test accept more than one file, file extension should be .java or .kt
 */
abstract class AbstractHierarchyTest : KotlinHierarchyViewTestBase() {
    protected var folderName: String? = null

    @Throws(Exception::class)
    protected fun doTypeClassHierarchyTest(folderName: String) {
        this.folderName = folderName
        doHierarchyTest(typeHierarchyStructure, *filesToConfigure)
    }

    @Throws(Exception::class)
    protected fun doSuperClassHierarchyTest(folderName: String) {
        this.folderName = folderName
        doHierarchyTest(superTypesHierarchyStructure, *filesToConfigure)
    }

    @Throws(Exception::class)
    protected fun doSubClassHierarchyTest(folderName: String) {
        this.folderName = folderName
        doHierarchyTest(subTypesHierarchyStructure, *filesToConfigure)
    }

    @Throws(Exception::class)
    protected fun doCallerHierarchyTest(folderName: String) {
        this.folderName = folderName
        doHierarchyTest(callerHierarchyStructure, *filesToConfigure)
    }

    @Throws(Exception::class)
    protected fun doCallerJavaHierarchyTest(folderName: String) {
        this.folderName = folderName
        doHierarchyTest(callerJavaHierarchyStructure, *filesToConfigure)
    }

    @Throws(Exception::class)
    protected fun doCalleeHierarchyTest(folderName: String) {
        this.folderName = folderName
        doHierarchyTest(calleeHierarchyStructure, *filesToConfigure)
    }

    @Throws(Exception::class)
    protected fun doOverrideHierarchyTest(folderName: String) {
        this.folderName = folderName
        doHierarchyTest(overrideHierarchyStructure, *filesToConfigure)
    }

    private val superTypesHierarchyStructure: Computable<HierarchyTreeStructure>
        private get() = Computable {
            SupertypesHierarchyTreeStructure(
                project,
                getElementAtCaret(LanguageTypeHierarchy.INSTANCE) as PsiClass
            )
        }

    private val subTypesHierarchyStructure: Computable<HierarchyTreeStructure>
        private get() = Computable {
            SubtypesHierarchyTreeStructure(
                project,
                getElementAtCaret(LanguageTypeHierarchy.INSTANCE) as PsiClass,
                HierarchyBrowserBaseEx.SCOPE_PROJECT
            )
        }

    private val typeHierarchyStructure: Computable<HierarchyTreeStructure>
        private get() = Computable {
            TypeHierarchyTreeStructure(
                project,
                getElementAtCaret(LanguageTypeHierarchy.INSTANCE) as PsiClass,
                HierarchyBrowserBaseEx.SCOPE_PROJECT
            )
        }

    private val callerHierarchyStructure: Computable<HierarchyTreeStructure>
        private get() = Computable {
            KotlinCallerTreeStructure(
                (getElementAtCaret(LanguageCallHierarchy.INSTANCE) as KtElement),
                HierarchyBrowserBaseEx.SCOPE_PROJECT
            )
        }

    private val callerJavaHierarchyStructure: Computable<HierarchyTreeStructure>
        private get() = Computable {
            CallerMethodsTreeStructure(
                project,
                (getElementAtCaret(LanguageCallHierarchy.INSTANCE) as PsiMember),
                HierarchyBrowserBaseEx.SCOPE_PROJECT
            )
        }

    private val calleeHierarchyStructure: Computable<HierarchyTreeStructure>
        private get() = Computable {
            KotlinCalleeTreeStructure(
                (getElementAtCaret(LanguageCallHierarchy.INSTANCE) as KtElement),
                HierarchyBrowserBaseEx.SCOPE_PROJECT
            )
        }

    private val overrideHierarchyStructure: Computable<HierarchyTreeStructure>
        private get() = Computable {
            KotlinOverrideTreeStructure(
                project,
                (getElementAtCaret(LanguageMethodHierarchy.INSTANCE) as KtCallableDeclaration)
            )
        }

    private fun getElementAtCaret(extension: LanguageExtension<HierarchyProvider>): PsiElement {
        val file =
            PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
        val provider =
            BrowseHierarchyActionBase.findProvider(extension, file, file, dataContext)
        return provider?.getTarget(dataContext)
            ?: throw RefactoringErrorHintException("Cannot apply action for element at caret")
    }

    private val dataContext: DataContext
        private get() {
            val editor = editor
            val context = MapDataContext()
            context.put(CommonDataKeys.PROJECT, project)
            context.put(CommonDataKeys.EDITOR, editor)
            val targetElement = TextEditorPsiDataProvider().getData(
                CommonDataKeys.PSI_ELEMENT.name,
                editor,
                editor.caretModel.currentCaret
            ) as PsiElement?
            context.put(CommonDataKeys.PSI_ELEMENT, targetElement)
            return context
        }

    protected val filesToConfigure: Array<String>
        protected get() {
            val files: MutableList<String> = ArrayList(2)
            FileUtil.processFilesRecursively(
                File(folderName)
            ) { file ->
                val fileName = file.name
                if (fileName.endsWith(".kt") || fileName.endsWith(".java")) {
                    files.add(fileName)
                }
                true
            }
            Collections.sort(files)
            return ArrayUtil.toStringArray(files)
        }

    @Throws(Exception::class)
    override fun doHierarchyTest(
        treeStructureComputable: Computable<out HierarchyTreeStructure>,
        vararg fileNames: String
    ) {
        try {
            super.doHierarchyTest(treeStructureComputable, *fileNames)
        } catch (e: RefactoringErrorHintException) {
            val file = File(folderName, "messages.txt")
            if (file.exists()) {
                val expectedMessage = FileUtil.loadFile(file, true)
                TestCase.assertEquals(expectedMessage, e.localizedMessage)
            } else {
                TestCase.fail("Unexpected error: " + e.localizedMessage)
            }
        } catch (failure: ComparisonFailure) {
            val actual = ComparisonDetailsExtractor.getActual(failure)
            val verificationFilePath = testDataPath + "/" + getTestName(false) + "_verification.xml"
            KotlinTestUtils.assertEqualsToFile(File(verificationFilePath), actual)
        }
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
    }

    override fun getTestDataPath(): String {
        val testRoot = super.getTestDataPath()
        val testDir = KotlinTestUtils.getTestDataFileName(this.javaClass, name)
        return "$testRoot/$testDir"
    }
}