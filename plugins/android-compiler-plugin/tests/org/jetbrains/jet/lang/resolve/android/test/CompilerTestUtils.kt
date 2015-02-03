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

package org.jetbrains.jet.lang.resolve.android.test

import com.intellij.openapi.project.Project
import org.jetbrains.jet.extensions.ExternalDeclarationsProvider
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.utils.emptyOrSingletonList
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment
import org.jetbrains.kotlin.android.AndroidConfigurationKeys
import org.jetbrains.jet.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.jet.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.android.AndroidExpressionCodegen
import org.jetbrains.jet.config.CompilerConfiguration
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.jet.lang.resolve.android.CliAndroidUIXmlProcessor

private class AndroidTestDeclarationsProvider(
        val project: Project,
        val resPath: String,
        val manifestPath: String
) : ExternalDeclarationsProvider {
    override fun getExternalDeclarations(): Collection<JetFile> {
        val parser = CliAndroidUIXmlProcessor(project, resPath, manifestPath)
        return emptyOrSingletonList(parser.parseToPsi(project))
    }
}

fun UsefulTestCase.createAndroidTestEnvironment(
        configuration: CompilerConfiguration,
        resPath: String,
        manifestPath: String): JetCoreEnvironment
{
    configuration.put(AndroidConfigurationKeys.ANDROID_RES_PATH, resPath)
    configuration.put(AndroidConfigurationKeys.ANDROID_MANIFEST, manifestPath)
    val myEnvironment = JetCoreEnvironment.createForTests(getTestRootDisposable()!!, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    val project = myEnvironment.getProject()
    ExternalDeclarationsProvider.registerExtension(project, AndroidTestDeclarationsProvider(project, resPath, manifestPath))
    ExpressionCodegenExtension.registerExtension(project, AndroidExpressionCodegen())
    return myEnvironment
}
