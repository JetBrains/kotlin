/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.NotFoundClasses
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.runtime.components.*
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.java.components.JavaResolverCache
import org.jetbrains.kotlin.load.java.lazy.SingleModuleClassResolver
import org.jetbrains.kotlin.load.kotlin.*
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.toMetadataVersion
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.jvm.ClassLoaderByConfiguration

class PackageFragmentFromClassLoaderProviderExtension(
    val classLoaderGetter: ClassLoaderByConfiguration,
    val scriptCompilationConfiguration: ScriptCompilationConfiguration,
    val compilerConfiguration: CompilerConfiguration
) : PackageFragmentProviderExtension {

    override fun getPackageFragmentProvider(
        project: Project,
        module: ModuleDescriptor,
        storageManager: StorageManager,
        trace: BindingTrace,
        moduleInfo: ModuleInfo?,
        lookupTracker: LookupTracker
    ): PackageFragmentProvider {
        val classLoader = classLoaderGetter(scriptCompilationConfiguration)

        val reflectKotlinClassFinder = ReflectKotlinClassFinder(classLoader)
        val deserializedDescriptorResolver = DeserializedDescriptorResolver()
        val singleModuleClassResolver = SingleModuleClassResolver()
        val notFoundClasses = NotFoundClasses(storageManager, module)
        val languageVersionSettings = compilerConfiguration.languageVersionSettings
        val packagePartProvider =
            PackagePartFromClassLoaderProvider(
                classLoader,
                languageVersionSettings,
                compilerConfiguration[CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY]!!
            )

        val lazyJavaPackageFragmentProvider =
            makeLazyJavaPackageFragmentProvider(
                ReflectJavaClassFinder(classLoader), module, storageManager, notFoundClasses,
                reflectKotlinClassFinder, deserializedDescriptorResolver,
                RuntimeErrorReporter, RuntimeSourceElementFactory, singleModuleClassResolver,
                packagePartProvider
            )

        val deserializationComponentsForJava =
            makeDeserializationComponentsForJava(
                module, storageManager, notFoundClasses, lazyJavaPackageFragmentProvider,
                reflectKotlinClassFinder, deserializedDescriptorResolver, RuntimeErrorReporter,
                languageVersionSettings.languageVersion.toMetadataVersion()
            )

        deserializedDescriptorResolver.setComponents(deserializationComponentsForJava)

        val javaDescriptorResolver = JavaDescriptorResolver(lazyJavaPackageFragmentProvider, JavaResolverCache.EMPTY)
        singleModuleClassResolver.resolver = javaDescriptorResolver

        return lazyJavaPackageFragmentProvider
    }
}

