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

package org.jetbrains.kotlin.idea.spring.tests.generate

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PathUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.codeInsight.generate.AbstractCodeInsightActionTest
import org.jetbrains.kotlin.idea.spring.generate.beanFilter
import org.jetbrains.kotlin.idea.spring.tests.SpringTestFixtureExtension
import org.jetbrains.kotlin.idea.test.TestFixtureExtension
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File

abstract class AbstractGenerateSpringDependencyActionTest : AbstractCodeInsightActionTest() {
    private var configFile: VirtualFile? = null
    private var configFilePath: String? = null

    override fun setUp() {
        super.setUp()
        TestFixtureExtension.loadFixture<SpringTestFixtureExtension>(myModule)
    }

    override fun configureExtra(mainFilePath: String, mainFileText: String) {
        val packagePath = mainFileText.lines().let { it.find { it.trim().startsWith("package") } }
                                  ?.removePrefix("package")
                                  ?.trim()?.replace(".", "/") ?: ""
        val mainFilePathInProject = packagePath + "/" + File(mainFilePath).name
        myFixture.addFileToProject(mainFilePathInProject, mainFileText)

        configFilePath = InTextDirectivesUtils.findStringWithPrefixes(mainFileText, "// CONFIG_FILE: ")?.let {
            "${PathUtil.getParentPath(mainFilePath)}/$it"
        } ?: mainFilePathInProject
        val springFileSet = TestFixtureExtension
                .getFixture<SpringTestFixtureExtension>()!!
                .configureFileSet(myFixture, listOf(configFilePath!!))
        if (configFilePath != mainFilePathInProject && PathUtil.getFileName(configFilePath!!) != "spring-config.xml") {
            configFile = springFileSet.files.single().file!!
        }

        val beansToChoose = InTextDirectivesUtils.findListWithPrefixes(mainFileText, "// CHOOSE_BEAN: ")
        project.beanFilter = { it.name in beansToChoose }
    }

    override fun checkExtra() {
        configFile?.let {
            myFixture.openFileInEditor(it)
            val afterFile = File("$configFilePath.after")
            TestCase.assertTrue(afterFile.exists())
            myFixture.checkResult(FileUtil.loadFile(afterFile, true))
        }
    }

    override fun tearDown() {
        configFile = null
        TestFixtureExtension.unloadFixture<SpringTestFixtureExtension>()
        super.tearDown()
    }
}