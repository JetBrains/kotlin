/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.move

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.testFramework.PsiTestUtil
import org.jetbrains.kotlin.idea.core.util.toPsiDirectory
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.MoveKotlinDeclarationsHandler
import org.jetbrains.kotlin.idea.test.KotlinMultiFileTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.extractMultipleMarkerOffsets
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.junit.internal.runners.JUnit38ClassRunner
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.junit.runner.RunWith
import java.nio.file.Path

@RunWith(JUnit38ClassRunner::class)
class MoveKotlinDeclarationsHandlerTest : KotlinMultiFileTestCase() {
    override fun getTestDataPath() = PluginTestCaseBase.getTestDataPathBase()

    override fun getTestRoot() = "/refactoring/moveHandler/declarations"

    private fun doTest(action: (rootDir: VirtualFile, handler: MoveKotlinDeclarationsHandler) -> Unit) {
        val filesToDelete = mutableListOf<Path>()

        try {
            val path = "$testDataPath$testRoot/${getTestName(true)}"
            val rootDir = PsiTestUtil.createTestProjectStructure(myProject, myModule, path, filesToDelete, false)
            prepareProject(rootDir)
            PsiDocumentManager.getInstance(myProject).commitAllDocuments()
            action(rootDir, MoveKotlinDeclarationsHandler())
        } finally {
            filesToDelete.forEach { it.toFile().deleteRecursively() }
        }
    }

    private fun getPsiDirectory(rootDir: VirtualFile, path: String) = rootDir.findFileByRelativePath(path)!!.toPsiDirectory(project)!!

    private fun getPsiFile(rootDir: VirtualFile, path: String) = rootDir.findFileByRelativePath(path)!!.toPsiFile(project)!!

    private fun getElementAtCaret(rootDir: VirtualFile, path: String) = getElementsAtCarets(rootDir, path).single()

    private fun getElementsAtCarets(rootDir: VirtualFile, path: String): List<PsiElement> {
        val file = getPsiFile(rootDir, path)
        val document = FileDocumentManager.getInstance().getDocument(file.virtualFile)!!
        return document.extractMultipleMarkerOffsets(project).map { file.findElementAt(it)!! }
    }

    fun testObjectLiteral() = doTest { rootDir, handler ->
        val objectDeclaration = getElementAtCaret(rootDir, "test.kt").getNonStrictParentOfType<KtObjectDeclaration>()!!
        assert(!handler.canMove(arrayOf<PsiElement>(objectDeclaration), null, null))
    }

    fun testLocalClass() = doTest { rootDir, handler ->
        val klass = getElementAtCaret(rootDir, "test.kt").getNonStrictParentOfType<KtClass>()!!
        assert(!handler.canMove(arrayOf<PsiElement>(klass), null, null))
    }

    fun testLocalFun() = doTest { rootDir, handler ->
        val function = getElementAtCaret(rootDir, "test.kt").getNonStrictParentOfType<KtNamedFunction>()!!
        assert(!handler.canMove(arrayOf<PsiElement>(function), null, null))
    }

    fun testLocalVal() = doTest { rootDir, handler ->
        val property = getElementAtCaret(rootDir, "test.kt").getNonStrictParentOfType<KtProperty>()!!
        assert(!handler.canMove(arrayOf<PsiElement>(property), null, null))
    }

    fun testMemberFun() = doTest { rootDir, handler ->
        val function = getElementAtCaret(rootDir, "test.kt").getNonStrictParentOfType<KtNamedFunction>()!!
        assert(!handler.canMove(arrayOf<PsiElement>(function), null, null))
    }

    fun testMemberVal() = doTest { rootDir, handler ->
        val property = getElementAtCaret(rootDir, "test.kt").getNonStrictParentOfType<KtProperty>()!!
        assert(!handler.canMove(arrayOf<PsiElement>(property), null, null))
    }

    fun testNestedClass() = doTest { rootDir, handler ->
        val klass = getElementAtCaret(rootDir, "test.kt").getNonStrictParentOfType<KtClass>()!!
        assert(handler.canMove(arrayOf<PsiElement>(klass), null, null))
    }

    fun testInnerClass() = doTest { rootDir, handler ->
        val klass = getElementAtCaret(rootDir, "test.kt").getNonStrictParentOfType<KtClass>()!!
        assert(handler.canMove(arrayOf<PsiElement>(klass), null, null))
    }

    fun testTopLevelClass() = doTest { rootDir, handler ->
        val klass = getElementAtCaret(rootDir, "test.kt").getNonStrictParentOfType<KtClass>()!!
        assert(handler.canMove(arrayOf<PsiElement>(klass), null, null))
    }

    fun testTopLevelFun() = doTest { rootDir, handler ->
        val function = getElementAtCaret(rootDir, "test.kt").getNonStrictParentOfType<KtNamedFunction>()!!
        assert(handler.canMove(arrayOf<PsiElement>(function), null, null))
    }

    fun testTopLevelVal() = doTest { rootDir, handler ->
        val property = getElementAtCaret(rootDir, "test.kt").getNonStrictParentOfType<KtProperty>()!!
        assert(handler.canMove(arrayOf<PsiElement>(property), null, null))
    }

    fun testMultipleNestedClasses() = doTest { rootDir, handler ->
        val classes = getElementsAtCarets(rootDir, "test.kt").map { it.getNonStrictParentOfType<KtClass>()!! }
        assert(handler.canMove(classes.toTypedArray(), null, null))
    }

    fun testNestedAndTopLevelClass() = doTest { rootDir, handler ->
        val classes = getElementsAtCarets(rootDir, "test.kt").map { it.getNonStrictParentOfType<KtClass>()!! }
        assert(!handler.canMove(classes.toTypedArray(), null, null))
    }

    fun testMultipleTopLevelDeclarations() = doTest { rootDir, handler ->
        val declarations = getElementsAtCarets(rootDir, "test.kt").map { it.getNonStrictParentOfType<KtNamedDeclaration>()!! }
        assert(handler.canMove(declarations.toTypedArray(), null, null))
    }

    fun testMultipleTopLevelDeclarationsInDifferentFiles() = doTest { rootDir, handler ->
        val declarations = listOf("test.kt", "test2.kt").flatMap { getElementsAtCarets(rootDir, it) }
            .map { it.getNonStrictParentOfType<KtNamedDeclaration>()!! }
        assert(handler.canMove(declarations.toTypedArray(), null, null))

        val files = listOf("test.kt", "test2.kt").map { getPsiFile(rootDir, it) }
        assert(handler.canMove(files.toTypedArray(), null, null))
    }

    fun testMultipleTopLevelDeclarationsInDifferentDirs() = doTest { rootDir, handler ->
        val declarations = listOf("test1/test.kt", "test2/test2.kt").flatMap { getElementsAtCarets(rootDir, it) }
            .map { it.getNonStrictParentOfType<KtNamedDeclaration>()!! }
        assert(!handler.canMove(declarations.toTypedArray(), null, null))

        val files = listOf("test1/test.kt", "test2/test2.kt").map { getPsiFile(rootDir, it) }
        assert(!handler.canMove(files.toTypedArray(), null, null))
    }

    fun testFileAndTopLevelDeclarations() = doTest { rootDir, handler ->
        val elements = getElementsAtCarets(rootDir, "test.kt").map { it.getNonStrictParentOfType<KtNamedDeclaration>()!! } + getPsiFile(
            rootDir,
            "test2.kt"
        )
        assert(!handler.canMove(elements.toTypedArray(), null, null))
    }

    fun testCommonTargets() = doTest { rootDir, handler ->
        val elementsToMove = arrayOf<PsiElement>(getElementAtCaret(rootDir, "test.kt").getNonStrictParentOfType<KtClass>()!!)

        val targetPackage = JavaPsiFacade.getInstance(project).findPackage("pack")!!
        assert(handler.canMove(elementsToMove, targetPackage, null))

        val targetDirectory = getPsiDirectory(rootDir, "pack")
        assert(handler.canMove(elementsToMove, targetDirectory, null))

        val targetFile = getPsiFile(rootDir, "pack/test2.kt")
        assert(handler.canMove(elementsToMove, targetFile, null))
    }

    fun testTopLevelClassToClass() = doTest { rootDir, handler ->
        val elementsToMove = arrayOf<PsiElement>(getElementAtCaret(rootDir, "test.kt").getNonStrictParentOfType<KtClass>()!!)
        val targetFile = getPsiFile(rootDir, "test2.kt") as KtFile

        val topLevelTarget = targetFile.declarations.firstIsInstance<KtClass>()
        assert(topLevelTarget.name == "B")
        assert(!handler.canMove(elementsToMove, topLevelTarget, null))

        val annotationTarget = targetFile.declarations.first { it.name == "Ann" } as KtClass
        assert(!handler.canMove(elementsToMove, annotationTarget, null))

        val nestedTarget = topLevelTarget.declarations.firstIsInstance<KtClass>()
        assert(nestedTarget.name == "C")
        assert(!handler.canMove(elementsToMove, nestedTarget, null))
    }

    fun testNestedClassToClass() = doTest { rootDir, handler ->
        val elementsToMove = arrayOf<PsiElement>(getElementAtCaret(rootDir, "test.kt").getNonStrictParentOfType<KtClass>()!!)
        val targetFile = getPsiFile(rootDir, "test2.kt") as KtFile

        val topLevelTarget = targetFile.declarations.firstIsInstance<KtClass>()
        assert(topLevelTarget.name == "B")
        assert(handler.canMove(elementsToMove, topLevelTarget, null))

        val annotationTarget = targetFile.declarations.first { it.name == "Ann" } as KtClass
        assert(!handler.canMove(elementsToMove, annotationTarget, null))

        val nestedTarget = topLevelTarget.declarations.firstIsInstance<KtClass>()
        assert(nestedTarget.name == "C")
        assert(handler.canMove(elementsToMove, nestedTarget, null))
    }

    fun testTypeAlias() = doTest { rootDir, handler ->
        val typeAlias = getElementAtCaret(rootDir, "test.kt").getNonStrictParentOfType<KtTypeAlias>()!!
        assert(handler.canMove(arrayOf<PsiElement>(typeAlias), null, null))
    }

    fun testTopLevelClassInScript() = doTest { rootDir, handler ->
        val klass = getElementAtCaret(rootDir, "test.kts").getNonStrictParentOfType<KtClass>()!!
        assert(handler.canMove(arrayOf<PsiElement>(klass), null, null))
    }

    fun testTopLevelFunInScript() = doTest { rootDir, handler ->
        val function = getElementAtCaret(rootDir, "test.kts").getNonStrictParentOfType<KtNamedFunction>()!!
        assert(handler.canMove(arrayOf<PsiElement>(function), null, null))
    }

    fun testTopLevelValInScript() = doTest { rootDir, handler ->
        val property = getElementAtCaret(rootDir, "test.kts").getNonStrictParentOfType<KtProperty>()!!
        assert(handler.canMove(arrayOf<PsiElement>(property), null, null))
    }
}