/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters.incremental

import org.jetbrains.kotlin.js.test.converters.JsIrBackendFacade
import org.jetbrains.kotlin.js.test.converters.JsKlibBackendFacade
import org.jetbrains.kotlin.js.test.utils.JsIrIncrementalDataProvider
import org.jetbrains.kotlin.js.test.utils.jsIrIncrementalDataProvider
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.frontend.classic.ModuleDescriptorProvider
import org.jetbrains.kotlin.test.frontend.classic.moduleDescriptorProvider
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*

@Suppress("warnings")
class RecompileModuleJsIrBackendFacade(
    testServices: TestServices
) : CommonRecompileModuleJsBackendFacade<ClassicFrontendOutputArtifact, IrBackendInput>(testServices, TargetBackend.JS_IR) {
    override fun TestConfigurationBuilder.configure(module: TestModule) {
        startingArtifactFactory = { testServices.dependencyProvider.getArtifact(module, BackendKinds.IrBackend) }

        facadeStep { JsKlibBackendFacade(it, firstTimeCompilation = false) }
        facadeStep { JsIrBackendFacade(it, firstTimeCompilation = false) }
    }

    override fun TestServices.register(module: TestModule) {
        register(ModuleDescriptorProvider::class, testServices.moduleDescriptorProvider)
        register(LibraryProvider::class, testServices.libraryProvider)
        register(JsIrIncrementalDataProvider::class, testServices.jsIrIncrementalDataProvider)
    }
}
