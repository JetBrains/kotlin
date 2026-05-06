/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.cli.pipeline.web.WebLoadedIrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.js.JsIrLoadingPipelinePhase
import org.jetbrains.kotlin.ir.backend.js.MainModule
import org.jetbrains.kotlin.js.test.utils.JsIrIncrementalDataProvider
import org.jetbrains.kotlin.test.backend.ir.DeserializedFromKlibBackendInput
import org.jetbrains.kotlin.test.backend.ir.IrDeserializerCliFacade
import org.jetbrains.kotlin.test.frontend.classic.ModuleDescriptorProvider
import org.jetbrains.kotlin.test.frontend.classic.moduleDescriptorProvider
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*

class JsIrDeserializerFacade(
    testServices: TestServices,
) : IrDeserializerCliFacade<JsIrLoadingPipelinePhase, WebLoadedIrPipelineArtifact>(testServices, JsIrLoadingPipelinePhase) {
    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(
            service(::ModuleDescriptorProvider),
            service(::JsIrIncrementalDataProvider),
            service(::LibraryProvider)
        )

    override fun transform(
        module: TestModule,
        inputArtifact: BinaryArtifacts.KLib,
    ): DeserializedFromKlibBackendInput<WebLoadedIrPipelineArtifact>? =
        super.transform(module, inputArtifact)?.also { output ->
            val modulesStructure = output.cliArtifact.moduleStructure
            val mainModule = modulesStructure.mainModule as MainModule.Klib
            val klibs = modulesStructure.klibs
            val mainModuleLib = klibs.included ?: error("No module with ${mainModule.libPath} found")

            // Some test downstream handlers like JsSourceMapPathRewriter expect a module descriptor to be present.
            testServices.moduleDescriptorProvider.replaceModuleDescriptorForModule(
                module,
                modulesStructure.getModuleDescriptor(mainModuleLib)
            )
            for (library in klibs.all) {
                testServices.libraryProvider.setDescriptorAndLibraryByName(
                    library.libraryFile.canonicalPath,
                    modulesStructure.getModuleDescriptor(library),
                    library
                )
            }
        }
}
