/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.run

import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiFile
import com.intellij.refactoring.RefactoringFactory
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesProcessor
import com.intellij.util.ActionRunner
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesManager
import org.jetbrains.kotlin.idea.run.script.standalone.KotlinStandaloneScriptRunConfiguration
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.stubindex.KotlinScriptFqnIndex
import org.jetbrains.kotlin.idea.test.KotlinCodeInsightTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.utils.PathUtil
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

        programParametersList.checkParameter("-script") { it.contains("simpleScript.kts") }
        programParametersList.checkParameter("-kotlin-home") { it == PathUtil.kotlinPathsForIdeaPlugin.homePath.path }
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

        ScriptDependenciesManager.updateScriptDependenciesSynchronously(myFile.virtualFile, project)

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

        ScriptDependenciesManager.updateScriptDependenciesSynchronously(myFile.virtualFile, project)

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

    private fun List<String>.checkParameter(name: String, condition: (String) -> Boolean) {
        val param = find { it == name } ?: throw AssertionError("Should pass $name to compiler")
        val paramValue = this[this.indexOf(param) + 1]
        Assert.assertTrue("Check for $name parameter fails: actual value = $paramValue", condition(paramValue))
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
