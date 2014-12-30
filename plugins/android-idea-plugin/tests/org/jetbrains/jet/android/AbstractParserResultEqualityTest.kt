/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.android

import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment
import org.jetbrains.jet.lang.resolve.android.CliAndroidUIXmlProcessor
import org.jetbrains.jet.JetTestUtils
import org.jetbrains.jet.ConfigurationKind
import org.jetbrains.jet.TestJdkKind
import org.jetbrains.jet.plugin.android.IDEAndroidUIXmlProcessor
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys
import kotlin.test.assertEquals
import org.jetbrains.jet.plugin.android.TestConst
import org.jetbrains.jet.plugin.PluginTestCaseBase
import org.jetbrains.kotlin.android.AndroidConfigurationKeys
import org.jetbrains.jet.cli.jvm.compiler.EnvironmentConfigFiles
import com.intellij.openapi.module.ModuleManager

public abstract class AbstractParserResultEqualityTest : KotlinAndroidTestCase() {
    public fun doTest(path: String) {
        val project = myFixture.getProject()
        project.putUserData(TestConst.TESTDATA_PATH, path)
        myFixture.copyDirectoryToProject(getResDir(), "res")
        val cliParser = CliAndroidUIXmlProcessor(project, path + "../AndroidManifest.xml", path + getResDir() + "/layout/")
        val ideParser = IDEAndroidUIXmlProcessor(ModuleManager.getInstance(project).getModules()[0])

        val cliResult = cliParser.parseToPsi()!!.getText()
        val ideResult = ideParser.parseToPsi()!!.getText()

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