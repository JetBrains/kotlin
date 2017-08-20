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
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.android.parcel.ParcelableCodegenExtension
import org.jetbrains.kotlin.android.parcel.ParcelableDeclarationChecker
import org.jetbrains.kotlin.android.parcel.ParcelableResolveExtension
import org.jetbrains.kotlin.android.synthetic.codegen.AndroidOnDestroyClassBuilderInterceptorExtension
import org.jetbrains.kotlin.android.synthetic.codegen.CliAndroidExtensionsExpressionCodegenExtension
import org.jetbrains.kotlin.android.synthetic.diagnostic.AndroidExtensionPropertiesCallChecker
import org.jetbrains.kotlin.android.synthetic.diagnostic.DefaultErrorMessagesAndroid
import org.jetbrains.kotlin.android.synthetic.res.AndroidLayoutXmlFileManager
import org.jetbrains.kotlin.android.synthetic.res.AndroidVariant
import org.jetbrains.kotlin.android.synthetic.res.CliAndroidLayoutXmlFileManager
import org.jetbrains.kotlin.android.synthetic.res.CliAndroidPackageFragmentProviderExtension
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
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.android.synthetic.codegen.ParcelableClinitClassBuilderInterceptorExtension
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension

object AndroidConfigurationKeys {
    val VARIANT: CompilerConfigurationKey<List<String>> = CompilerConfigurationKey.create<List<String>>("Android build variant")
    val PACKAGE: CompilerConfigurationKey<String> = CompilerConfigurationKey.create<String>("application package fq name")
    val EXPERIMENTAL: CompilerConfigurationKey<String> = CompilerConfigurationKey.create<String>("enable experimental features")
}

class AndroidCommandLineProcessor : CommandLineProcessor {
    companion object {
        val ANDROID_COMPILER_PLUGIN_ID: String = "org.jetbrains.kotlin.android"

        val VARIANT_OPTION: CliOption = CliOption("variant", "<name;path>", "Android build variant", allowMultipleOccurrences = true)
        val PACKAGE_OPTION: CliOption = CliOption("package", "<fq name>", "Application package")
        val EXPERIMENTAL_OPTION: CliOption = CliOption("experimental", "true/false", "Enable experimental features", required = false)

        /* This option is just for saving Android Extensions status in Kotlin facet. It should not be supported from CLI. */
        val ENABLED_OPTION: CliOption = CliOption("enabled", "true/false", "Enable Android Extensions", required = false)
    }

    override val pluginId: String = ANDROID_COMPILER_PLUGIN_ID

    override val pluginOptions: Collection<CliOption> = listOf(VARIANT_OPTION, PACKAGE_OPTION, EXPERIMENTAL_OPTION)

    override fun processOption(option: CliOption, value: String, configuration: CompilerConfiguration) {
        when (option) {
            VARIANT_OPTION -> configuration.appendList(AndroidConfigurationKeys.VARIANT, value)
            PACKAGE_OPTION -> configuration.put(AndroidConfigurationKeys.PACKAGE, value)
            EXPERIMENTAL_OPTION -> configuration.put(AndroidConfigurationKeys.EXPERIMENTAL, value)
            else -> throw CliOptionProcessingException("Unknown option: ${option.name}")
        }
    }
}

class AndroidComponentRegistrar : ComponentRegistrar {
    companion object {
        fun registerParcelExtensions(project: Project) {
            ExpressionCodegenExtension.registerExtension(project, ParcelableCodegenExtension())
            SyntheticResolveExtension.registerExtension(project, ParcelableResolveExtension())
            ClassBuilderInterceptorExtension.registerExtension(project, ParcelableClinitClassBuilderInterceptorExtension())
        }
    }

    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        registerParcelExtensions(project)

        val applicationPackage = configuration.get(AndroidConfigurationKeys.PACKAGE)
        val variants = configuration.get(AndroidConfigurationKeys.VARIANT)?.mapNotNull { parseVariant(it) } ?: emptyList()
        val isExperimental = configuration.get(AndroidConfigurationKeys.EXPERIMENTAL) == "true"

        if (variants.isNotEmpty() && !applicationPackage.isNullOrBlank()) {
            val layoutXmlFileManager = CliAndroidLayoutXmlFileManager(project, applicationPackage!!, variants)
            project.registerService(AndroidLayoutXmlFileManager::class.java, layoutXmlFileManager)

            ExpressionCodegenExtension.registerExtension(project, CliAndroidExtensionsExpressionCodegenExtension(isExperimental))
            StorageComponentContainerContributor.registerExtension(project, AndroidExtensionPropertiesComponentContainerContributor())
            Extensions.getRootArea().getExtensionPoint(DefaultErrorMessages.Extension.EP_NAME).registerExtension(DefaultErrorMessagesAndroid())
            ClassBuilderInterceptorExtension.registerExtension(project, AndroidOnDestroyClassBuilderInterceptorExtension())
            PackageFragmentProviderExtension.registerExtension(project, CliAndroidPackageFragmentProviderExtension(isExperimental))
        }
    }

    private fun parseVariant(s: String): AndroidVariant? {
        val parts = s.split(';')
        if (parts.size < 2) return null
        return AndroidVariant(parts[0], parts.drop(1))
    }
}

class AndroidExtensionPropertiesComponentContainerContributor : StorageComponentContainerContributor {
    override fun addDeclarations(container: StorageComponentContainer, platform: TargetPlatform) {
        if (platform is JvmPlatform) {
            container.useInstance(AndroidExtensionPropertiesCallChecker())
            container.useInstance(ParcelableDeclarationChecker())
        }
    }
}