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

package org.jetbrains.jet.lang.resolve.android

import org.jetbrains.jet.codegen.AbstractBytecodeTextTest
import org.jetbrains.jet.JetTestUtils
import org.jetbrains.jet.ConfigurationKind
import org.jetbrains.jet.TestJdkKind
import org.jetbrains.jet.config.CompilerConfiguration
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment
import org.jetbrains.kotlin.android.AndroidConfigurationKeys
import org.jetbrains.jet.extensions.ExternalDeclarationsProvider
import org.jetbrains.jet.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.android.AndroidExpressionCodegen
import org.jetbrains.jet.cli.jvm.compiler.EnvironmentConfigFiles

public abstract class AbstractAndroidBytecodeShapeTest : AbstractBytecodeTextTest() {

    private fun createAndroidAPIEnvironment(path: String) {
        return createEnvironmentForConfiguration(JetTestUtils.compilerConfigurationForTests(ConfigurationKind.ALL, TestJdkKind.ANDROID_API), path)
    }

    private fun createFakeAndroidEnvironment(path: String) {
        return createEnvironmentForConfiguration(JetTestUtils.compilerConfigurationForTests(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK), path)
    }

    private fun createEnvironmentForConfiguration(configuration: CompilerConfiguration, path: String) {
        val resPath = path + "res/layout/"
        val manifestPath = path + "../AndroidManifest.xml"
        configuration.put(AndroidConfigurationKeys.ANDROID_RES_PATH, resPath)
        configuration.put(AndroidConfigurationKeys.ANDROID_MANIFEST, manifestPath)
        myEnvironment = JetCoreEnvironment.createForTests(getTestRootDisposable()!!, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        val project = myEnvironment.getProject()
        ExternalDeclarationsProvider.registerExtension(project, AndroidTestDeclarationsProvider(project, resPath, manifestPath))
        ExpressionCodegenExtension.registerExtension(project, AndroidExpressionCodegen())
    }

    public override fun doTest(path: String) {
        val fileName = path + getTestName(true) + ".kt"
        createAndroidAPIEnvironment(path)
        loadFileByFullPath(fileName)
        val expected = readExpectedOccurrences(fileName)
        countAndCompareActualOccurrences(expected)
    }
}
