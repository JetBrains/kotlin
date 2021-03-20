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

package org.jetbrains.kotlin.frontend.js.di

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.frontend.di.configureIncrementalCompilation
import org.jetbrains.kotlin.frontend.di.configureModule
import org.jetbrains.kotlin.frontend.di.configureStandardResolveComponents
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.js.resolve.JsPlatformAnalyzerServices
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.TargetEnvironment
import org.jetbrains.kotlin.resolve.createContainer
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory

fun createContainerForJS(
    moduleContext: ModuleContext,
    bindingTrace: BindingTrace,
    declarationProviderFactory: DeclarationProviderFactory,
    languageVersionSettings: LanguageVersionSettings,
    lookupTracker: LookupTracker,
    expectActualTracker: ExpectActualTracker,
    additionalPackages: List<PackageFragmentProvider>,
    targetEnvironment: TargetEnvironment,
): StorageComponentContainer {
    val storageComponentContainer = createContainer("TopDownAnalyzerForJs", JsPlatformAnalyzerServices) {
        configureModule(
            moduleContext,
            JsPlatforms.defaultJsPlatform,
            JsPlatformAnalyzerServices,
            bindingTrace,
            languageVersionSettings
        )

        configureIncrementalCompilation(lookupTracker, expectActualTracker)
        configureStandardResolveComponents()

        useInstance(declarationProviderFactory)
        targetEnvironment.configure(this)
    }.apply {
        val packagePartProviders = mutableListOf(get<KotlinCodeAnalyzer>().packageFragmentProvider)
        val moduleDescriptor = get<ModuleDescriptorImpl>()
        packagePartProviders += additionalPackages
        moduleDescriptor.initialize(CompositePackageFragmentProvider(packagePartProviders))
    }
    return storageComponentContainer
}

fun createTopDownAnalyzerForJs(
    moduleContext: ModuleContext,
    bindingTrace: BindingTrace,
    declarationProviderFactory: DeclarationProviderFactory,
    languageVersionSettings: LanguageVersionSettings,
    lookupTracker: LookupTracker,
    expectActualTracker: ExpectActualTracker,
    additionalPackages: List<PackageFragmentProvider>,
    targetEnvironment: TargetEnvironment,
): LazyTopDownAnalyzer {
    return createContainerForJS(
        moduleContext, bindingTrace, declarationProviderFactory, languageVersionSettings,
        lookupTracker, expectActualTracker, additionalPackages, targetEnvironment
    ).get<LazyTopDownAnalyzer>()
}
