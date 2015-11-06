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

import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.registerServiceInstance
import org.jetbrains.kotlin.android.synthetic.AndroidConfigurationKeys
import org.jetbrains.kotlin.android.synthetic.AndroidExtensionPropertiesComponentContainerContributor
import org.jetbrains.kotlin.android.synthetic.codegen.AndroidExpressionCodegenExtension
import org.jetbrains.kotlin.android.synthetic.codegen.AndroidOnDestroyClassBuilderInterceptorExtension
import org.jetbrains.kotlin.android.synthetic.res.AndroidLayoutXmlFileManager
import org.jetbrains.kotlin.android.synthetic.res.AndroidVariant
import org.jetbrains.kotlin.android.synthetic.res.CliAndroidLayoutXmlFileManager
import org.jetbrains.kotlin.android.synthetic.res.CliAndroidPackageFragmentProviderExtension
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import java.io.File

fun UsefulTestCase.createAndroidTestEnvironment(configuration: CompilerConfiguration, resDirectories: List<String>): KotlinCoreEnvironment {
    configuration.put(AndroidConfigurationKeys.VARIANT, resDirectories)
    configuration.put(AndroidConfigurationKeys.PACKAGE, "test")

    val myEnvironment = KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    val project = myEnvironment.project

    val variants = listOf(AndroidVariant.createMainVariant(resDirectories))
    project.registerServiceInstance(AndroidLayoutXmlFileManager::class.java, CliAndroidLayoutXmlFileManager(project, "test", variants))

    ExpressionCodegenExtension.registerExtension(project, AndroidExpressionCodegenExtension())
    StorageComponentContainerContributor.registerExtension(project, AndroidExtensionPropertiesComponentContainerContributor())
    ClassBuilderInterceptorExtension.registerExtension(project, AndroidOnDestroyClassBuilderInterceptorExtension())
    PackageFragmentProviderExtension.registerExtension(project, CliAndroidPackageFragmentProviderExtension())

    return myEnvironment
}

fun getResPaths(path: String): List<String> {
    return File(path).listFiles { it.name.startsWith("res") && it.isDirectory }!!.map { "$path${it.name}/" }
}