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

import org.jetbrains.kotlin.container.createContainer
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.container.useImpl
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.frontend.di.configureModule
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.BodyResolveCache
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzerForTopLevel
import org.jetbrains.kotlin.resolve.lazy.FileScopeProviderImpl
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.kotlin.types.DynamicTypesAllowed

public fun createTopDownAnalyzerForJs(
        moduleContext: ModuleContext,
        bindingTrace: BindingTrace,
        declarationProviderFactory: DeclarationProviderFactory
): LazyTopDownAnalyzerForTopLevel {
    val storageComponentContainer = createContainer("TopDownAnalyzerForJs") {
        configureModule(moduleContext, JsPlatform, bindingTrace)

        useInstance(declarationProviderFactory)
        useImpl<FileScopeProviderImpl>()
        useInstance(BodyResolveCache.ThrowException)
        useInstance(LookupTracker.DO_NOTHING)
        useImpl<ResolveSession>()
        useImpl<LazyTopDownAnalyzerForTopLevel>()
    }
    return storageComponentContainer.get<LazyTopDownAnalyzerForTopLevel>()
}

