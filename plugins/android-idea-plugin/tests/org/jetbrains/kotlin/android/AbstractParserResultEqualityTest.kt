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

import org.jetbrains.kotlin.cli.jvm.compiler.JetCoreEnvironment
import org.jetbrains.kotlin.plugin.android.TestConst
import org.jetbrains.kotlin.lang.resolve.android.CliAndroidUIXmlProcessor
import com.intellij.openapi.module.ModuleManager
import org.jetbrains.kotlin.plugin.android.IDEAndroidUIXmlProcessor
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.JetTestUtils
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import kotlin.test.assertEquals

public abstract class AbstractParserResultEqualityTest : KotlinAndroidTestCase() {
    public fun doTest(path: String) {
        val project = myFixture.getProject()
        project.putUserData(TestConst.TESTDATA_PATH, path)
        myFixture.copyDirectoryToProject(getResDir(), "res")
        val cliParser = CliAndroidUIXmlProcessor(project, path + "../AndroidManifest.xml", path + getResDir() + "/layout/")
        val ideParser = IDEAndroidUIXmlProcessor(ModuleManager.getInstance(project).getModules()[0])

        val cliResult = cliParser.parseToPsi()!!.joinToString("\n\n")
        val ideResult = ideParser.parseToPsi()!!.joinToString("\n\n")

        assertEquals(cliResult, ideResult)
    }

    private fun getEnvironment(testPath: String): JetCoreEnvironment {
        val configuration = JetTestUtils.compilerConfigurationForTests(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK)
                configuration.put<String>(AndroidConfigurationKeys.ANDROID_RES_PATH, testPath + "/layout")
                configuration.put<String>(AndroidConfigurationKeys.ANDROID_MANIFEST, testPath + "/AndroidManifest.xml")
        return JetCoreEnvironment.createForTests(getTestRootDisposable()!!, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    }

    override fun getTestDataPath(): String? {
        return KotlinAndroidTestCaseBase.getPluginTestDataPathBase() + "/parserResultEquality/" + getTestName(true) + "/"
    }
}