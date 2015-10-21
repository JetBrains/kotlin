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

package org.jetbrains.kotlin.android.synthetic

import com.intellij.mock.MockProject
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.android.synthetic.codegen.AndroidExpressionCodegenExtension
import org.jetbrains.kotlin.android.synthetic.codegen.AndroidOnDestroyClassBuilderInterceptorExtension
import org.jetbrains.kotlin.android.synthetic.diagnostic.AndroidExtensionPropertiesCallChecker
import org.jetbrains.kotlin.android.synthetic.diagnostic.DefaultErrorMessagesAndroid
import org.jetbrains.kotlin.android.synthetic.res.*
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.extensions.ExternalDeclarationsProvider
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform

public object AndroidConfigurationKeys {
    public val VARIANT: CompilerConfigurationKey<List<String>> = CompilerConfigurationKey.create<List<String>>("Android build variant")
    public val PACKAGE: CompilerConfigurationKey<String> = CompilerConfigurationKey.create<String>("application package fq name")
}

public class AndroidCommandLineProcessor : CommandLineProcessor {
    companion object {
        public val ANDROID_COMPILER_PLUGIN_ID: String = "org.jetbrains.kotlin.android"

        public val VARIANT_OPTION: CliOption = CliOption("variant", "<name;path>", "Android build variant", allowMultipleOccurrences = true)
        public val PACKAGE_OPTION: CliOption = CliOption("package", "<fq name>", "Application package")
    }

    override val pluginId: String = ANDROID_COMPILER_PLUGIN_ID

    override val pluginOptions: Collection<CliOption> = listOf(VARIANT_OPTION, PACKAGE_OPTION)

    override fun processOption(option: CliOption, value: String, configuration: CompilerConfiguration) {
        when (option) {
            VARIANT_OPTION -> {
                val paths = configuration.getList(AndroidConfigurationKeys.VARIANT).toArrayList()
                paths.add(value)
                configuration.put(AndroidConfigurationKeys.VARIANT, paths)
            }
            PACKAGE_OPTION -> configuration.put(AndroidConfigurationKeys.PACKAGE, value)
            else -> throw CliOptionProcessingException("Unknown option: ${option.name}")
        }
    }
}

public class CliAndroidDeclarationsProvider(private val project: Project) : ExternalDeclarationsProvider {
    override fun getExternalDeclarations(moduleInfo: ModuleInfo?): Collection<KtFile> {
        val parser = ServiceManager.getService(project, SyntheticFileGenerator::class.java) as? CliSyntheticFileGenerator
        return parser?.getSyntheticFiles() ?: listOf()
    }
}

public class AndroidComponentRegistrar : ComponentRegistrar {

    public override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val applicationPackage = configuration.get(AndroidConfigurationKeys.PACKAGE)
        val variants = configuration.get(AndroidConfigurationKeys.VARIANT)?.map { parseVariant(it) }?.filterNotNull() ?: emptyList()

        if (variants.isNotEmpty() && !applicationPackage.isNullOrBlank()) {
            val xmlProcessor = CliSyntheticFileGenerator(project, applicationPackage!!, variants)

            project.registerService(SyntheticFileGenerator::class.java, xmlProcessor)

            val layoutXmlFileManager = CliAndroidLayoutXmlFileManager(project, applicationPackage, variants)
            project.registerService(AndroidLayoutXmlFileManager::class.java, layoutXmlFileManager)

            ExternalDeclarationsProvider.registerExtension(project, CliAndroidDeclarationsProvider(project))
            ExpressionCodegenExtension.registerExtension(project, AndroidExpressionCodegenExtension())
            StorageComponentContainerContributor.registerExtension(project, AndroidExtensionPropertiesComponentContainerContributor())
            Extensions.getRootArea().getExtensionPoint(DefaultErrorMessages.Extension.EP_NAME).registerExtension(DefaultErrorMessagesAndroid())
            ClassBuilderInterceptorExtension.registerExtension(project, AndroidOnDestroyClassBuilderInterceptorExtension())
        }
    }

    private fun parseVariant(s: String): AndroidVariant? {
        val parts = s.split(';')
        if (parts.size < 2) return null
        return AndroidVariant(parts[0], parts.drop(0))
    }
}

public class AndroidExtensionPropertiesComponentContainerContributor : StorageComponentContainerContributor {
    override fun addDeclarations(container: StorageComponentContainer, platform: TargetPlatform) {
        if (platform is JvmPlatform) {
            container.useInstance(AndroidExtensionPropertiesCallChecker())
        }
    }
}