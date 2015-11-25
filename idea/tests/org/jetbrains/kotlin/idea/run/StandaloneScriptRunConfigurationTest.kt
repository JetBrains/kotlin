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

import com.intellij.refactoring.RefactoringFactory
import org.jetbrains.kotlin.idea.run.script.standalone.KotlinStandaloneScriptRunConfiguration
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.stubindex.KotlinScriptFqnIndex
import org.jetbrains.kotlin.idea.test.KotlinCodeInsightTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.junit.Assert
import kotlin.test.assertNotEquals

class StandaloneScriptRunConfigurationTest : KotlinCodeInsightTestCase() {

    fun testConfigurationForScript() {
        configureByFile("run/simpleScript.kts")
        val script = KotlinScriptFqnIndex.instance.get("foo.SimpleScript", project, project.allScope()).single()
        val runConfiguration = createConfigurationFromElement(script) as KotlinStandaloneScriptRunConfiguration

        Assert.assertEquals(script.containingFile.virtualFile.canonicalPath, runConfiguration.filePath)
        Assert.assertEquals("SimpleScript", runConfiguration.name)

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

        Assert.assertEquals("SimpleScript", runConfiguration.name)
        val originalPath = script.containingFile.virtualFile.canonicalPath
        assertEquals(originalPath, runConfiguration.filePath)

        RefactoringFactory.getInstance(project).createRename(script.containingFile, "renamedScript.kts").run()

        Assert.assertEquals("RenamedScript", runConfiguration.name)
        assertEquals(script.containingFile.virtualFile.canonicalPath, runConfiguration.filePath)
        assertNotEquals(originalPath, runConfiguration.filePath)
    }

    override fun getTestDataPath() = PluginTestCaseBase.getTestDataPathBase() + "/run/StandaloneScript/"
    override fun getTestProjectJdk() = PluginTestCaseBase.mockJdk()
}
