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

package org.jetbrains.kotlin.android

import com.intellij.openapi.module.ModuleManager
import org.jetbrains.kotlin.android.synthetic.idea.TestConst
import org.jetbrains.kotlin.android.synthetic.idea.res.IDESyntheticFileGenerator
import org.jetbrains.kotlin.android.synthetic.res.AndroidVariant
import org.jetbrains.kotlin.android.synthetic.res.CliSyntheticFileGenerator

public abstract class AbstractParserResultEqualityTest : KotlinAndroidTestCase() {
    public fun doTest(path: String) {
        val project = myFixture.project
        project.putUserData(TestConst.TESTDATA_PATH, path)
        val resDirs = getResourceDirs(path).map {
            myFixture.copyDirectoryToProject(it.name, it.name)
            "$path${it.name}/"
        }

        val variants = listOf(AndroidVariant.createMainVariant(resDirs))
        val cliParser = CliSyntheticFileGenerator(project, "$path../AndroidManifest.xml", variants)
        val ideParser = IDESyntheticFileGenerator(ModuleManager.getInstance(project).modules[0])

        val cliResult = cliParser.getSyntheticFiles().joinToString("\n\n")
        val ideResult = ideParser.getSyntheticFiles().joinToString("\n\n")

        assertEquals(cliResult, ideResult)
    }
    
    override fun getTestDataPath(): String? {
        return KotlinAndroidTestCaseBase.getPluginTestDataPathBase() + "/parserResultEquality/" + getTestName(true) + "/"
    }
}