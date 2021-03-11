/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.trackers

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.testFramework.PsiTestUtil
import junit.framework.Assert
import org.jetbrains.kotlin.idea.stubs.AbstractMultiModuleTest
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.util.sourceRoots
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.trackers.KotlinOutOfBlockModificationTrackerFactory
import org.jetbrains.kotlin.trackers.createModuleWithoutDependenciesOutOfBlockModificationTracker
import java.io.File
import org.jetbrains.kotlin.trackers.createProjectWideOutOfBlockModificationTracker
import java.nio.file.Files
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.writeText

class KotlinModuleOutOfBlockTrackerTest : AbstractMultiModuleTest() {
    override fun getTestDataDirectory(): File = error("Should not be called")

    fun testThatModuleOutOfBlockChangeInfluenceOnlySingleModule() {
        val moduleA = createModuleWithModificationTracker("a") {
            listOf(
                FileWithText("main.kt", "fun main() = 10")
            )
        }
        val moduleB = createModuleWithModificationTracker("b")
        val moduleC = createModuleWithModificationTracker("c")


        val moduleAWithTracker = ModuleWithModificationTracker(moduleA)
        val moduleBWithTracker = ModuleWithModificationTracker(moduleB)
        val moduleCWithTracker = ModuleWithModificationTracker(moduleC)

        moduleA.typeInFunctionBody("main.kt", textAfterTyping = "fun main() = hello10")

        Assert.assertTrue(
            "Out of block modification count for module A with out of block should change after typing, modification count is ${moduleAWithTracker.modificationCount}",
            moduleAWithTracker.changed()
        )
        Assert.assertFalse(
            "Out of block modification count for module B without out of block should not change after typing, modification count is ${moduleBWithTracker.modificationCount}",
            moduleBWithTracker.changed()
        )
        Assert.assertFalse(
            "Out of block modification count for module C without out of block should not change after typing, modification count is ${moduleCWithTracker.modificationCount}",
            moduleCWithTracker.changed()
        )
    }

    fun testThatInEveryModuleOutOfBlockWillHappenAfterContentRootChange() {
        val moduleA = createModuleWithModificationTracker("a")
        val moduleB = createModuleWithModificationTracker("b")
        val moduleC = createModuleWithModificationTracker("c")

        val moduleAWithTracker = ModuleWithModificationTracker(moduleA)
        val moduleBWithTracker = ModuleWithModificationTracker(moduleB)
        val moduleCWithTracker = ModuleWithModificationTracker(moduleC)

        runWriteAction {
            moduleA.sourceRoots.first().createChildData(/* requestor = */ null, "file.kt")
        }

        Assert.assertTrue(
            "Out of block modification count for module A should change after content root change, modification count is ${moduleAWithTracker.modificationCount}",
            moduleAWithTracker.changed()
        )
        Assert.assertTrue(
            "Out of block modification count for module B should change after content root change, modification count is ${moduleBWithTracker.modificationCount}",
            moduleBWithTracker.changed()
        )
        Assert.assertTrue(
            "Out of block modification count for module C should change after content root change modification count is ${moduleCWithTracker.modificationCount}",
            moduleCWithTracker.changed()
        )
    }

    fun testThatNonPhysicalFileChangeNotCausingBOOM() {
        val moduleA = createModuleWithModificationTracker("a") {
            listOf(
                FileWithText("main.kt", "fun main() {}")
            )
        }
        val moduleB = createModuleWithModificationTracker("b")

        val moduleAWithTracker = ModuleWithModificationTracker(moduleA)
        val moduleBWithTracker = ModuleWithModificationTracker(moduleB)


        val projectWithModificationTracker = ProjectWithModificationTracker(project)

        runWriteAction {
            val nonPhysicalPsi = KtPsiFactory(moduleA.project).createFile("nonPhysical", "val a = c")
            nonPhysicalPsi.add(KtPsiFactory(moduleA.project).createFunction("fun x(){}"))
        }

        Assert.assertFalse(
            "Out of block modification count for module A should not change after non physical file change, modification count is ${moduleAWithTracker.modificationCount}",
            moduleAWithTracker.changed()
        )
        Assert.assertFalse(
            "Out of block modification count for module B should not change after non physical file change, modification count is ${moduleBWithTracker.modificationCount}",
            moduleBWithTracker.changed()
        )

        Assert.assertFalse(
            "Out of block modification count for project should not change after non physical file change, modification count is ${projectWithModificationTracker.modificationCount}",
            projectWithModificationTracker.changed()
        )
    }

    private fun Module.typeInFunctionBody(fileName: String, textAfterTyping: String) {
        val file = "${sourceRoots.first().url}/$fileName"
        val virtualFile = VirtualFileManager.getInstance().findFileByUrl(file)!!
        val ktFile = PsiManager.getInstance(project).findFile(virtualFile) as KtFile
        configureByExistingFile(virtualFile)

        val singleFunction = ktFile.declarations.single() as KtNamedFunction

        editor.caretModel.moveToOffset(singleFunction.bodyExpression!!.textOffset)
        type("hello")
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        Assert.assertEquals(textAfterTyping, ktFile.text)
    }

    @OptIn(ExperimentalPathApi::class)
    private fun createModuleWithModificationTracker(
        name: String,
        createFiles: () -> List<FileWithText> = { emptyList() },
    ): Module {
        val tmpDir = createTempDirectory().toPath()
        createFiles().forEach { file ->
            Files.createFile(tmpDir.resolve(file.name)).writeText(file.text)
        }
        val module: Module = createModule("$tmpDir/$name", moduleType)
        val root = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tmpDir.toFile())!!
        WriteCommandAction.writeCommandAction(module.project).run<RuntimeException> {
            root.refresh(false, true)
        }

        PsiTestUtil.addSourceContentToRoots(module, root)
        return module
    }

    private data class FileWithText(val name: String, val text: String)

    abstract class WithModificationTracker(protected val modificationTracker: ModificationTracker) {
        private val initialModificationCount = modificationTracker.modificationCount
        val modificationCount: Long get() = modificationTracker.modificationCount

        fun changed(): Boolean =
            modificationTracker.modificationCount != initialModificationCount
    }

    private class ModuleWithModificationTracker(module: Module) : WithModificationTracker(
        module.createModuleWithoutDependenciesOutOfBlockModificationTracker()
    )

    private class ProjectWithModificationTracker(project: Project) : WithModificationTracker(
        project.createProjectWideOutOfBlockModificationTracker()
    )
}