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

import com.intellij.openapi.project.Project
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.android.synthetic.AndroidConfigurationKeys
import org.jetbrains.kotlin.android.synthetic.AndroidExtensionPropertiesComponentContainerContributor
import org.jetbrains.kotlin.android.synthetic.codegen.AndroidExpressionCodegenExtension
import org.jetbrains.kotlin.android.synthetic.codegen.AndroidOnDestroyClassBuilderInterceptorExtension
import org.jetbrains.kotlin.android.synthetic.res.AndroidSyntheticFile
import org.jetbrains.kotlin.android.synthetic.res.AndroidVariant
import org.jetbrains.kotlin.android.synthetic.res.CliSyntheticFileGenerator
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.ExternalDeclarationsProvider
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

private class AndroidTestExternalDeclarationsProvider(
        val project: Project,
        val resPaths: List<String>,
        val manifestPath: String,
        val supportV4: Boolean
) : ExternalDeclarationsProvider {
    override fun getExternalDeclarations(moduleInfo: ModuleInfo?): Collection<KtFile> {
        val parser = CliSyntheticFileGeneratorForConversionTest(project, manifestPath, resPaths, supportV4)
        return parser.getSyntheticFiles()
    }
}

class CliSyntheticFileGeneratorForConversionTest(
        project: Project,
        manifestPath: String,
        resDirectories: List<String>,
        private val supportV4: Boolean
) : CliSyntheticFileGenerator(project, manifestPath, listOf(AndroidVariant.createMainVariant(resDirectories))) {

    fun gen() = generateSyntheticFiles(false, supportV4)

    public override fun generateSyntheticFiles(generateCommonFiles: Boolean, supportV4: Boolean): List<AndroidSyntheticFile> {
        return super.generateSyntheticFiles(generateCommonFiles, this.supportV4)
    }
}

fun UsefulTestCase.createAndroidTestEnvironment(
        configuration: CompilerConfiguration,
        resPaths: List<String>,
        manifestPath: String,
        supportV4: Boolean
): KotlinCoreEnvironment {
    configuration.put(AndroidConfigurationKeys.VARIANT, resPaths)
    configuration.put(AndroidConfigurationKeys.PACKAGE, manifestPath)

    val myEnvironment = KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    val project = myEnvironment.project

    val declarationsProvider = AndroidTestExternalDeclarationsProvider(project, resPaths, manifestPath, supportV4)
    ExternalDeclarationsProvider.registerExtension(project, declarationsProvider)
    ExpressionCodegenExtension.registerExtension(project, AndroidExpressionCodegenExtension())
    StorageComponentContainerContributor.registerExtension(project, AndroidExtensionPropertiesComponentContainerContributor())
    ClassBuilderInterceptorExtension.registerExtension(project, AndroidOnDestroyClassBuilderInterceptorExtension())

    return myEnvironment
}

fun getResPaths(path: String): List<String> {
    return File(path).listFiles { it.name.startsWith("res") && it.isDirectory }!!.map { "$path${it.name}/" }
}