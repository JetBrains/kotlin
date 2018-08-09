/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring.move

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.testFramework.PsiTestUtil
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.MoveKotlinDeclarationsHandler
import org.jetbrains.kotlin.idea.refactoring.toPsiDirectory
import org.jetbrains.kotlin.idea.refactoring.toPsiFile
import org.jetbrains.kotlin.idea.test.KotlinMultiFileTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.extractMultipleMarkerOffsets
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

class MoveKotlinDeclarationsHandlerTest : KotlinMultiFileTestCase() {
    override fun getTestDataPath() = PluginTestCaseBase.getTestDataPathBase()

    override fun getTestRoot() = "/refactoring/moveHandler/declarations"

    private fun doTest(action: (rootDir: VirtualFile, handler: MoveKotlinDeclarationsHandler) -> Unit) {
        val path = "$testDataPath$testRoot/${getTestName(true)}"
        val rootDir = PsiTestUtil.createTestProjectStructure(myProject, myModule, path, myFilesToDelete, false)
        prepareProject(rootDir)
        PsiDocumentManager.getInstance(myProject).commitAllDocuments()
        action(rootDir, MoveKotlinDeclarationsHandler())
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
        assert(!handler.canMove(arrayOf<PsiElement>(objectDeclaration), null))
    }

    fun testLocalClass() = doTest { rootDir, handler ->
        val klass = getElementAtCaret(rootDir, "test.kt").getNonStrictParentOfType<KtClass>()!!
        assert(!handler.canMove(arrayOf<PsiElement>(klass), null))
    }

    fun testLocalFun() = doTest { rootDir, handler ->
        val function = getElementAtCaret(rootDir, "test.kt").getNonStrictParentOfType<KtNamedFunction>()!!
        assert(!handler.canMove(arrayOf<PsiElement>(function), null))
    }

    fun testLocalVal() = doTest { rootDir, handler ->
        val property = getElementAtCaret(rootDir, "test.kt").getNonStrictParentOfType<KtProperty>()!!
        assert(!handler.canMove(arrayOf<PsiElement>(property), null))
    }

    fun testMemberFun() = doTest { rootDir, handler ->
        val function = getElementAtCaret(rootDir, "test.kt").getNonStrictParentOfType<KtNamedFunction>()!!
        assert(!handler.canMove(arrayOf<PsiElement>(function), null))
    }

    fun testMemberVal() = doTest { rootDir, handler ->
        val property = getElementAtCaret(rootDir, "test.kt").getNonStrictParentOfType<KtProperty>()!!
        assert(!handler.canMove(arrayOf<PsiElement>(property), null))
    }

    fun testNestedClass() = doTest { rootDir, handler ->
        val klass = getElementAtCaret(rootDir, "test.kt").getNonStrictParentOfType<KtClass>()!!
        assert(handler.canMove(arrayOf<PsiElement>(klass), null))
    }

    fun testInnerClass() = doTest { rootDir, handler ->
        val klass = getElementAtCaret(rootDir, "test.kt").getNonStrictParentOfType<KtClass>()!!
        assert(handler.canMove(arrayOf<PsiElement>(klass), null))
    }

    fun testTopLevelClass() = doTest { rootDir, handler ->
        val klass = getElementAtCaret(rootDir, "test.kt").getNonStrictParentOfType<KtClass>()!!
        assert(handler.canMove(arrayOf<PsiElement>(klass), null))
    }

    fun testTopLevelFun() = doTest { rootDir, handler ->
        val function = getElementAtCaret(rootDir, "test.kt").getNonStrictParentOfType<KtNamedFunction>()!!
        assert(handler.canMove(arrayOf<PsiElement>(function), null))
    }

    fun testTopLevelVal() = doTest { rootDir, handler ->
        val property = getElementAtCaret(rootDir, "test.kt").getNonStrictParentOfType<KtProperty>()!!
        assert(handler.canMove(arrayOf<PsiElement>(property), null))
    }

    fun testMultipleNestedClasses() = doTest { rootDir, handler ->
        val classes = getElementsAtCarets(rootDir, "test.kt").map { it.getNonStrictParentOfType<KtClass>()!! }
        assert(handler.canMove(classes.toTypedArray(), null))
    }

    fun testNestedAndTopLevelClass() = doTest { rootDir, handler ->
        val classes = getElementsAtCarets(rootDir, "test.kt").map { it.getNonStrictParentOfType<KtClass>()!! }
        assert(!handler.canMove(classes.toTypedArray(), null))
    }

    fun testMultipleTopLevelDeclarations() = doTest { rootDir, handler ->
        val declarations = getElementsAtCarets(rootDir, "test.kt").map { it.getNonStrictParentOfType<KtNamedDeclaration>()!! }
        assert(handler.canMove(declarations.toTypedArray(), null))
    }

    fun testMultipleTopLevelDeclarationsInDifferentFiles() = doTest { rootDir, handler ->
        val declarations = listOf("test.kt", "test2.kt")
                .flatMap { getElementsAtCarets(rootDir, it) }
                .map { it.getNonStrictParentOfType<KtNamedDeclaration>()!! }
        assert(handler.canMove(declarations.toTypedArray(), null))

        val files = listOf("test.kt", "test2.kt").map { getPsiFile(rootDir, it) }
        assert(handler.canMove(files.toTypedArray(), null))
    }

    fun testMultipleTopLevelDeclarationsInDifferentDirs() = doTest { rootDir, handler ->
        val declarations = listOf("test1/test.kt", "test2/test2.kt")
                .flatMap { getElementsAtCarets(rootDir, it) }
                .map { it.getNonStrictParentOfType<KtNamedDeclaration>()!! }
        assert(!handler.canMove(declarations.toTypedArray(), null))

        val files = listOf("test1/test.kt", "test2/test2.kt").map { getPsiFile(rootDir, it) }
        assert(!handler.canMove(files.toTypedArray(), null))
    }

    fun testFileAndTopLevelDeclarations() = doTest { rootDir, handler ->
        val elements = getElementsAtCarets(rootDir, "test.kt").map { it.getNonStrictParentOfType<KtNamedDeclaration>()!! } +
                       getPsiFile(rootDir, "test2.kt")
        assert(!handler.canMove(elements.toTypedArray(), null))
    }

    fun testCommonTargets() = doTest { rootDir, handler ->
        val elementsToMove = arrayOf<PsiElement>(getElementAtCaret(rootDir, "test.kt").getNonStrictParentOfType<KtClass>()!!)

        val targetPackage = JavaPsiFacade.getInstance(project).findPackage("pack")!!
        assert(handler.canMove(elementsToMove, targetPackage))

        val targetDirectory = getPsiDirectory(rootDir, "pack")
        assert(handler.canMove(elementsToMove, targetDirectory))

        val targetFile = getPsiFile(rootDir, "pack/test2.kt")
        assert(handler.canMove(elementsToMove, targetFile))
    }

    fun testTopLevelClassToClass() = doTest { rootDir, handler ->
        val elementsToMove = arrayOf<PsiElement>(getElementAtCaret(rootDir, "test.kt").getNonStrictParentOfType<KtClass>()!!)
        val targetFile = getPsiFile(rootDir, "test2.kt") as KtFile

        val topLevelTarget = targetFile.declarations.firstIsInstance<KtClass>()
        assert(topLevelTarget.name == "B")
        assert(!handler.canMove(elementsToMove, topLevelTarget))

        val annotationTarget = targetFile.declarations.first { it.name == "Ann" } as KtClass
        assert(!handler.canMove(elementsToMove, annotationTarget))

        val nestedTarget = topLevelTarget.declarations.firstIsInstance<KtClass>()
        assert(nestedTarget.name == "C")
        assert(!handler.canMove(elementsToMove, nestedTarget))
    }

    fun testNestedClassToClass() = doTest { rootDir, handler ->
        val elementsToMove = arrayOf<PsiElement>(getElementAtCaret(rootDir, "test.kt").getNonStrictParentOfType<KtClass>()!!)
        val targetFile = getPsiFile(rootDir, "test2.kt") as KtFile

        val topLevelTarget = targetFile.declarations.firstIsInstance<KtClass>()
        assert(topLevelTarget.name == "B")
        assert(handler.canMove(elementsToMove, topLevelTarget))

        val annotationTarget = targetFile.declarations.first { it.name == "Ann" } as KtClass
        assert(!handler.canMove(elementsToMove, annotationTarget))

        val nestedTarget = topLevelTarget.declarations.firstIsInstance<KtClass>()
        assert(nestedTarget.name == "C")
        assert(handler.canMove(elementsToMove, nestedTarget))
    }

    fun testTypeAlias() = doTest { rootDir, handler ->
        val typeAlias = getElementAtCaret(rootDir, "test.kt").getNonStrictParentOfType<KtTypeAlias>()!!
        assert(handler.canMove(arrayOf<PsiElement>(typeAlias), null))
    }

    fun testTopLevelClassInScript() = doTest { rootDir, handler ->
        val klass = getElementAtCaret(rootDir, "test.kts").getNonStrictParentOfType<KtClass>()!!
        assert(handler.canMove(arrayOf<PsiElement>(klass), null))
    }

    fun testTopLevelFunInScript() = doTest { rootDir, handler ->
        val function = getElementAtCaret(rootDir, "test.kts").getNonStrictParentOfType<KtNamedFunction>()!!
        assert(handler.canMove(arrayOf<PsiElement>(function), null))
    }

    fun testTopLevelValInScript() = doTest { rootDir, handler ->
        val property = getElementAtCaret(rootDir, "test.kts").getNonStrictParentOfType<KtProperty>()!!
        assert(handler.canMove(arrayOf<PsiElement>(property), null))
    }
}