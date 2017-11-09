/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.run

import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiFile
import com.intellij.refactoring.RefactoringFactory
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesProcessor
import com.intellij.util.ActionRunner
import org.jetbrains.kotlin.idea.run.script.standalone.KotlinStandaloneScriptRunConfiguration
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.stubindex.KotlinScriptFqnIndex
import org.jetbrains.kotlin.idea.test.KotlinCodeInsightTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.KtScript
import org.junit.Assert
import kotlin.test.assertNotEquals

class StandaloneScriptRunConfigurationTest : KotlinCodeInsightTestCase() {

    fun testConfigurationForScript() {
        configureByFile("run/simpleScript.kts")
        val script = KotlinScriptFqnIndex.instance.get("foo.SimpleScript", project, project.allScope()).single()
        val runConfiguration = createConfigurationFromElement(script) as KotlinStandaloneScriptRunConfiguration

        Assert.assertEquals(script.containingFile.virtualFile.canonicalPath, runConfiguration.filePath)
        Assert.assertEquals("simpleScript.kts", runConfiguration.name)

        val javaParameters = getJavaRunParameters(runConfiguration)
        val programParametersList = javaParameters.programParametersList.list
        val (first, second) = programParametersList
        Assert.assertEquals("Should pass -script to compiler", "-script", first)
        Assert.assertTrue("Should pass script file to compiler", second.contains("simpleScript.kts"))
    }

    fun testOnFileRename() {
        configureByFile("renameFile/simpleScript.kts")
        val script = KotlinScriptFqnIndex.instance.get("foo.SimpleScript", project, project.allScope()).single()
        val runConfiguration = createConfigurationFromElement(script, save = true) as KotlinStandaloneScriptRunConfiguration

        Assert.assertEquals("simpleScript.kts", runConfiguration.name)
        val scriptVirtualFileBefore = script.containingFile.virtualFile
        val originalPath = scriptVirtualFileBefore.canonicalPath
        val originalWorkingDirectory = scriptVirtualFileBefore.parent.canonicalPath
        assertEquals(originalPath, runConfiguration.filePath)
        assertEquals(originalWorkingDirectory, runConfiguration.workingDirectory)

        RefactoringFactory.getInstance(project).createRename(script.containingFile, "renamedScript.kts").run()

        Assert.assertEquals("renamedScript.kts", runConfiguration.name)
        val scriptVirtualFileAfter = script.containingFile.virtualFile

        assertEquals(scriptVirtualFileAfter.canonicalPath, runConfiguration.filePath)
        assertNotEquals(originalPath, runConfiguration.filePath)

        assertEquals(scriptVirtualFileAfter.parent.canonicalPath, runConfiguration.workingDirectory)
        assertEquals(originalWorkingDirectory, runConfiguration.workingDirectory)
    }

    fun testOnFileMoveWithDefaultWorkingDir() {
        configureByFile("move/script.kts")
        val script = KotlinScriptFqnIndex.instance.get("foo.Script", project, project.allScope()).single()
        val runConfiguration = createConfigurationFromElement(script, save = true) as KotlinStandaloneScriptRunConfiguration

        Assert.assertEquals("script.kts", runConfiguration.name)
        val scriptVirtualFileBefore = script.containingFile.virtualFile
        val originalPath = scriptVirtualFileBefore.canonicalPath
        val originalWorkingDirectory = scriptVirtualFileBefore.parent.canonicalPath
        assertEquals(originalPath, runConfiguration.filePath)
        assertEquals(originalWorkingDirectory, runConfiguration.workingDirectory)

        moveScriptFile(script.containingFile)

        Assert.assertEquals("script.kts", runConfiguration.name)
        val scriptVirtualFileAfter = script.containingFile.virtualFile

        assertEquals(scriptVirtualFileAfter.canonicalPath, runConfiguration.filePath)
        assertNotEquals(originalPath, runConfiguration.filePath)

        assertEquals(scriptVirtualFileAfter.parent.canonicalPath, runConfiguration.workingDirectory)
        assertNotEquals(originalWorkingDirectory, runConfiguration.workingDirectory)
    }

    fun testOnFileMoveWithNonDefaultWorkingDir() {
        configureByFile("move/script.kts")
        val script = KotlinScriptFqnIndex.instance.get("foo.Script", project, project.allScope()).single()
        val runConfiguration = createConfigurationFromElement(script, save = true) as KotlinStandaloneScriptRunConfiguration

        Assert.assertEquals("script.kts", runConfiguration.name)
        runConfiguration.workingDirectory = runConfiguration.workingDirectory + "/customWorkingDirectory"
        val scriptVirtualFileBefore = script.containingFile.virtualFile
        val originalPath = scriptVirtualFileBefore.canonicalPath
        val originalWorkingDirectory = scriptVirtualFileBefore.parent.canonicalPath + "/customWorkingDirectory"

        assertEquals(originalPath, runConfiguration.filePath)
        assertEquals(originalWorkingDirectory, runConfiguration.workingDirectory)

        moveScriptFile(script.containingFile)

        Assert.assertEquals("script.kts", runConfiguration.name)
        val scriptVirtualFileAfter = script.containingFile.virtualFile

        assertEquals(scriptVirtualFileAfter.canonicalPath, runConfiguration.filePath)
        assertNotEquals(originalPath, runConfiguration.filePath)

        assertNotEquals(scriptVirtualFileAfter.parent.canonicalPath, runConfiguration.workingDirectory)
        assertEquals(originalWorkingDirectory, runConfiguration.workingDirectory)
    }

    fun moveScriptFile(scriptFile: PsiFile) {
        ActionRunner.runInsideWriteAction { VfsUtil.createDirectoryIfMissing(scriptFile.virtualFile.parent, "dest") }

        MoveFilesOrDirectoriesProcessor(
                project,
                arrayOf(scriptFile),
                JavaPsiFacade.getInstance(project).findPackage("dest")!!.directories[0],
                false, true, null, null
        ).run()
    }


    override fun getTestDataPath() = PluginTestCaseBase.getTestDataPathBase() + "/run/StandaloneScript/"
    override fun getTestProjectJdk() = PluginTestCaseBase.mockJdk()
}
