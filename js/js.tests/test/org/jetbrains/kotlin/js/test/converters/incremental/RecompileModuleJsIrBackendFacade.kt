/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters.incremental

import org.jetbrains.kotlin.js.test.converters.JsUnifiedIrDeserializerAndLoweringFacade
import org.jetbrains.kotlin.js.test.converters.JsKlibSerializerFacade
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
        startingArtifactFactory = {
            testServices.artifactsProvider.getArtifact(module, BackendKinds.IrBackend).also {
                require(it is IrBackendInput.JsIrAfterFrontendBackendInput) {
                    "Recompilation can start only from IC cache entry, which has type JsIrAfterFrontendBackendInput.\n" +
                    "Actual type: ${it::javaClass.name}.\nProbable cause: accidental override of artifact with the output of Klib deserialization facade"
                }
            }
        }

        facadeStep { JsKlibSerializerFacade(it, firstTimeCompilation = false) }
        facadeStep { JsUnifiedIrDeserializerAndLoweringFacade(it, firstTimeCompilation = false) }
    }

    override fun TestServices.register(module: TestModule) {
        register(ModuleDescriptorProvider::class, testServices.moduleDescriptorProvider)
        register(LibraryProvider::class, testServices.libraryProvider)
        register(JsIrIncrementalDataProvider::class, testServices.jsIrIncrementalDataProvider)
    }
}
