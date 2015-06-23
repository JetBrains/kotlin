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

package org.jetbrains.kotlin.lang.resolve.android.test

import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.psi.impl.PsiTreeChangePreprocessor
import org.jetbrains.kotlin.extensions.ExternalDeclarationsProvider
import org.jetbrains.kotlin.android.AndroidConfigurationKeys
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.android.AndroidExpressionCodegenExtension
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.lang.resolve.android.CliAndroidUIXmlProcessor
import java.io.File

private class AndroidTestExternalDeclarationsProvider(
        val project: Project,
        val resPaths: List<String>,
        val manifestPath: String,
        val supportV4: Boolean
) : ExternalDeclarationsProvider {
    override fun getExternalDeclarations(moduleInfo: ModuleInfo?): Collection<JetFile> {
        val parser = CliAndroidUIXmlProcessor(project, manifestPath, resPaths)
        parser.supportV4 = supportV4
        return parser.parseToPsi() ?: listOf()
    }
}

fun UsefulTestCase.createAndroidTestEnvironment(
        configuration: CompilerConfiguration,
        resPaths: List<String>,
        manifestPath: String,
        supportV4: Boolean
): KotlinCoreEnvironment {
    configuration.put(AndroidConfigurationKeys.ANDROID_RES_PATH, resPaths)
    configuration.put(AndroidConfigurationKeys.ANDROID_MANIFEST, manifestPath)

    val myEnvironment = KotlinCoreEnvironment.createForTests(getTestRootDisposable()!!, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    val project = myEnvironment.project

    val declarationsProvider = AndroidTestExternalDeclarationsProvider(project, resPaths, manifestPath, supportV4)
    ExternalDeclarationsProvider.registerExtension(project, declarationsProvider)
    ExpressionCodegenExtension.registerExtension(project, AndroidExpressionCodegenExtension())

    return myEnvironment
}

fun getResPaths(path: String): List<String> {
    return File(path).listFiles { it.name.startsWith("res") && it.isDirectory() }!!.map { "$path${it.name}/" }
}