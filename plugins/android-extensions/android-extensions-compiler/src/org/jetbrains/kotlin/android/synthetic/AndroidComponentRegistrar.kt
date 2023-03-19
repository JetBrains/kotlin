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
import org.jetbrains.kotlin.android.parcel.*
import org.jetbrains.kotlin.android.synthetic.codegen.CliAndroidExtensionsExpressionCodegenExtension
import org.jetbrains.kotlin.android.synthetic.codegen.CliAndroidIrExtension
import org.jetbrains.kotlin.android.synthetic.codegen.CliAndroidOnDestroyClassBuilderInterceptorExtension
import org.jetbrains.kotlin.android.synthetic.codegen.ParcelableClinitClassBuilderInterceptorExtension
import org.jetbrains.kotlin.android.synthetic.diagnostic.AndroidExtensionPropertiesCallChecker
import org.jetbrains.kotlin.android.synthetic.res.AndroidLayoutXmlFileManager
import org.jetbrains.kotlin.android.synthetic.res.AndroidVariant
import org.jetbrains.kotlin.android.synthetic.res.CliAndroidLayoutXmlFileManager
import org.jetbrains.kotlin.android.synthetic.res.CliAndroidPackageFragmentProviderExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.utils.decodePluginOptions

object AndroidConfigurationKeys {
    val VARIANT = CompilerConfigurationKey.create<List<String>>("Android build variant")
    val PACKAGE = CompilerConfigurationKey.create<String>("application package fq name")
    val EXPERIMENTAL = CompilerConfigurationKey.create<String>("enable experimental features")
    val DEFAULT_CACHE_IMPL = CompilerConfigurationKey.create<String>("default cache implementation")
    val FEATURES = CompilerConfigurationKey.create<Set<AndroidExtensionsFeature>>("enabled features")
}

enum class AndroidExtensionsFeature(val featureName: String) {
    VIEWS("views"),
    PARCELIZE("parcelize")
}

class AndroidCommandLineProcessor : CommandLineProcessor {
    companion object {
        val ANDROID_COMPILER_PLUGIN_ID: String = "org.jetbrains.kotlin.android"

        val CONFIGURATION = CliOption("configuration", "<encoded>", "Encoded configuration", required = false)

        val VARIANT_OPTION = CliOption("variant", "<name;path>", "Android build variant", allowMultipleOccurrences = true, required = false)
        val PACKAGE_OPTION = CliOption("package", "<fq name>", "Application package", required = false)
        val EXPERIMENTAL_OPTION = CliOption("experimental", "true/false", "Enable experimental features", required = false)
        val DEFAULT_CACHE_IMPL_OPTION = CliOption(
                "defaultCacheImplementation", "hashMap/sparseArray/none", "Default cache implementation for module", required = false)

        val FEATURES_OPTION = CliOption(
                "features", AndroidExtensionsFeature.values().joinToString(" | "), "Enabled features", required = false)

        /* This option is just for saving Android Extensions status in Kotlin facet. It should not be supported from CLI. */
        val ENABLED_OPTION: CliOption = CliOption("enabled", "true/false", "Enable Android Extensions", required = false)
    }

    override val pluginId: String = ANDROID_COMPILER_PLUGIN_ID

    override val pluginOptions: Collection<AbstractCliOption>
            = listOf(VARIANT_OPTION, PACKAGE_OPTION, EXPERIMENTAL_OPTION, DEFAULT_CACHE_IMPL_OPTION, CONFIGURATION, FEATURES_OPTION)

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option) {
            VARIANT_OPTION -> configuration.appendList(AndroidConfigurationKeys.VARIANT, value)
            PACKAGE_OPTION -> configuration.put(AndroidConfigurationKeys.PACKAGE, value)
            EXPERIMENTAL_OPTION -> configuration.put(AndroidConfigurationKeys.EXPERIMENTAL, value)
            DEFAULT_CACHE_IMPL_OPTION -> configuration.put(AndroidConfigurationKeys.DEFAULT_CACHE_IMPL, value)
            CONFIGURATION -> configuration.applyOptionsFrom(decodePluginOptions(value), pluginOptions)
            FEATURES_OPTION -> {
                val features = value.split(',').mapNotNullTo(mutableSetOf()) {
                    name -> AndroidExtensionsFeature.values().firstOrNull { it.featureName == name }
                }
                configuration.put(AndroidConfigurationKeys.FEATURES, features)
            }
            else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
        }
    }
}

@Suppress("DEPRECATION")
class AndroidComponentRegistrar : ComponentRegistrar {
    companion object {
        fun reportRemovedError(configuration: CompilerConfiguration) {
            val errorMessage =
                "The Android extensions ('kotlin-android-extensions') compiler plugin is no longer supported. " +
                        "Please use kotlin parcelize and view binding. " +
                        "More information: https://goo.gle/kotlin-android-extensions-deprecation"
            configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
                ?.report(CompilerMessageSeverity.ERROR, errorMessage, null)
                ?: throw IllegalStateException(errorMessage)
        }

        fun registerParcelExtensions(project: Project) {
            ExpressionCodegenExtension.registerExtension(project, ParcelableCodegenExtension())
            IrGenerationExtension.registerExtension(project, ParcelableIrGeneratorExtension())
            SyntheticResolveExtension.registerExtension(project, ParcelableResolveExtension())
            @Suppress("DEPRECATION_ERROR")
            org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension.registerExtension(
                project, ParcelableClinitClassBuilderInterceptorExtension()
            )
            StorageComponentContainerContributor.registerExtension(project, ParcelizeDeclarationCheckerComponentContainerContributor())
        }

        private fun parseVariant(s: String): AndroidVariant? {
            val parts = s.split(';')
            if (parts.size < 2) return null
            return AndroidVariant(parts[0], parts.drop(1))
        }

        fun registerViewExtensions(configuration: CompilerConfiguration, isExperimental: Boolean, project: MockProject) {
            val applicationPackage = configuration.get(AndroidConfigurationKeys.PACKAGE) ?: return
            val variants = configuration.get(AndroidConfigurationKeys.VARIANT)?.mapNotNull { parseVariant(it) } ?: return
            val globalCacheImpl = parseCacheImplementationType(configuration.get(AndroidConfigurationKeys.DEFAULT_CACHE_IMPL))

            if (variants.isEmpty() || applicationPackage.isEmpty()) {
                return
            }

            val layoutXmlFileManager = CliAndroidLayoutXmlFileManager(project, applicationPackage, variants)
            project.registerService(AndroidLayoutXmlFileManager::class.java, layoutXmlFileManager)

            ExpressionCodegenExtension.registerExtension(project,
                    CliAndroidExtensionsExpressionCodegenExtension(isExperimental, globalCacheImpl))

            IrGenerationExtension.registerExtension(project,
                    CliAndroidIrExtension(isExperimental, globalCacheImpl))

            StorageComponentContainerContributor.registerExtension(project,
                    AndroidExtensionPropertiesComponentContainerContributor())

            @Suppress("DEPRECATION_ERROR")
            org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension.registerExtension(
                project, CliAndroidOnDestroyClassBuilderInterceptorExtension(globalCacheImpl)
            )

            PackageFragmentProviderExtension.registerExtension(project,
                    CliAndroidPackageFragmentProviderExtension(isExperimental))
        }

        fun parseCacheImplementationType(s: String?): CacheImplementation = when (s) {
            "sparseArray" -> CacheImplementation.SPARSE_ARRAY
            "none" -> CacheImplementation.NO_CACHE
            else -> CacheImplementation.DEFAULT
        }
    }

    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        reportRemovedError(configuration)

        val features = configuration.get(AndroidConfigurationKeys.FEATURES) ?: AndroidExtensionsFeature.values().toSet()
        val isExperimental = configuration.get(AndroidConfigurationKeys.EXPERIMENTAL) == "true"

        if (AndroidExtensionsFeature.PARCELIZE in features) {
            registerParcelExtensions(project)
        }

        if (AndroidExtensionsFeature.VIEWS in features) {
            registerViewExtensions(configuration, isExperimental, project)
        }
    }
}

class AndroidExtensionPropertiesComponentContainerContributor : StorageComponentContainerContributor {
    override fun registerModuleComponents(
        container: StorageComponentContainer, platform: TargetPlatform, moduleDescriptor: ModuleDescriptor
    ) {
        if (platform.isJvm()) {
            container.useInstance(AndroidExtensionPropertiesCallChecker())
        }
    }
}

class ParcelizeDeclarationCheckerComponentContainerContributor : StorageComponentContainerContributor {
    override fun registerModuleComponents(
        container: StorageComponentContainer, platform: TargetPlatform, moduleDescriptor: ModuleDescriptor
    ) {
        if (platform.isJvm()) {
            container.useInstance(ParcelableDeclarationChecker())
            container.useInstance(ParcelableAnnotationChecker())
        }
    }
}
