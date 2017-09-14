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
import com.intellij.openapi.project.Project
import kotlinx.android.extensions.CacheImplementation
import org.jetbrains.kotlin.android.parcel.ParcelableCodegenExtension
import org.jetbrains.kotlin.android.parcel.ParcelableDeclarationChecker
import org.jetbrains.kotlin.android.parcel.ParcelableResolveExtension
import org.jetbrains.kotlin.android.synthetic.codegen.CliAndroidExtensionsExpressionCodegenExtension
import org.jetbrains.kotlin.android.synthetic.codegen.CliAndroidOnDestroyClassBuilderInterceptorExtension
import org.jetbrains.kotlin.android.synthetic.codegen.ParcelableClinitClassBuilderInterceptorExtension
import org.jetbrains.kotlin.android.synthetic.diagnostic.AndroidExtensionPropertiesCallChecker
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
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform

object AndroidConfigurationKeys {
    val VARIANT = CompilerConfigurationKey.create<List<String>>("Android build variant")
    val PACKAGE = CompilerConfigurationKey.create<String>("application package fq name")
    val EXPERIMENTAL = CompilerConfigurationKey.create<String>("enable experimental features")
    val DEFAULT_CACHE_IMPL = CompilerConfigurationKey.create<String>("default cache implementation")
}

class AndroidCommandLineProcessor : CommandLineProcessor {
    companion object {
        val ANDROID_COMPILER_PLUGIN_ID: String = "org.jetbrains.kotlin.android"

        val VARIANT_OPTION = CliOption("variant", "<name;path>", "Android build variant", allowMultipleOccurrences = true)
        val PACKAGE_OPTION = CliOption("package", "<fq name>", "Application package")
        val EXPERIMENTAL_OPTION = CliOption("experimental", "true/false", "Enable experimental features", required = false)
        val DEFAULT_CACHE_IMPL_OPTION = CliOption(
                "defaultCacheImplementation", "hashMap/sparseArray/none", "Default cache implementation for module", required = false)

        /* This option is just for saving Android Extensions status in Kotlin facet. It should not be supported from CLI. */
        val ENABLED_OPTION: CliOption = CliOption("enabled", "true/false", "Enable Android Extensions", required = false)
    }

    override val pluginId: String = ANDROID_COMPILER_PLUGIN_ID

    override val pluginOptions: Collection<CliOption>
            = listOf(VARIANT_OPTION, PACKAGE_OPTION, EXPERIMENTAL_OPTION, DEFAULT_CACHE_IMPL_OPTION)

    override fun processOption(option: CliOption, value: String, configuration: CompilerConfiguration) {
        when (option) {
            VARIANT_OPTION -> configuration.appendList(AndroidConfigurationKeys.VARIANT, value)
            PACKAGE_OPTION -> configuration.put(AndroidConfigurationKeys.PACKAGE, value)
            EXPERIMENTAL_OPTION -> configuration.put(AndroidConfigurationKeys.EXPERIMENTAL, value)
            DEFAULT_CACHE_IMPL_OPTION -> configuration.put(AndroidConfigurationKeys.DEFAULT_CACHE_IMPL, value)
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

        fun parseCacheImplementationType(s: String?): CacheImplementation = when (s) {
            "sparseArray" -> CacheImplementation.SPARSE_ARRAY
            "none" -> CacheImplementation.NO_CACHE
            else -> CacheImplementation.DEFAULT
        }
    }

    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val applicationPackage = configuration.get(AndroidConfigurationKeys.PACKAGE)
        val variants = configuration.get(AndroidConfigurationKeys.VARIANT)?.mapNotNull { parseVariant(it) } ?: emptyList()
        val isExperimental = configuration.get(AndroidConfigurationKeys.EXPERIMENTAL) == "true"
        val globalCacheImpl = parseCacheImplementationType(configuration.get(AndroidConfigurationKeys.DEFAULT_CACHE_IMPL))

        if (isExperimental) {
            registerParcelExtensions(project)
        }

        if (variants.isNotEmpty() && !applicationPackage.isNullOrBlank()) {
            val layoutXmlFileManager = CliAndroidLayoutXmlFileManager(project, applicationPackage!!, variants)
            project.registerService(AndroidLayoutXmlFileManager::class.java, layoutXmlFileManager)

            ExpressionCodegenExtension.registerExtension(project, CliAndroidExtensionsExpressionCodegenExtension(isExperimental, globalCacheImpl))
            StorageComponentContainerContributor.registerExtension(project, AndroidExtensionPropertiesComponentContainerContributor())
            ClassBuilderInterceptorExtension.registerExtension(project, CliAndroidOnDestroyClassBuilderInterceptorExtension(globalCacheImpl))
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
    override fun registerModuleComponents(
            container: StorageComponentContainer, platform: TargetPlatform, moduleDescriptor: ModuleDescriptor
    ) {
        if (platform != JvmPlatform) return

        container.useInstance(AndroidExtensionPropertiesCallChecker())
        container.useInstance(ParcelableDeclarationChecker())
    }
}