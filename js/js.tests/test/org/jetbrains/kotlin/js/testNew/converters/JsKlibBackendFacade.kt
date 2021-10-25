/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testNew.converters

import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.codegen.CompilerOutputSink
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.js.config.ErrorTolerancePolicy
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.backend.ir.IrBackendFacade
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.frontend.classic.moduleDescriptorProvider
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.jsLibraryProvider
import java.io.File

class JsKlibBackendFacade(
    testServices: TestServices
) : IrBackendFacade<BinaryArtifacts.Js>(testServices, ArtifactKinds.Js) {
    override fun transform(module: TestModule, inputArtifact: IrBackendInput): BinaryArtifacts.Js {
        if (inputArtifact !is IrBackendInput.JsIrBackendInput) {
            error("JsIrBackendFacade expects IrBackendInput.JsIrBackendInput as input")
        }

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val project = testServices.compilerConfigurationProvider.getProject(module)

        val outputFile = JsEnvironmentConfigurator.getJsKlibArtifactPath(testServices, module.name)
        val errorPolicy = configuration.get(JSConfigurationKeys.ERROR_TOLERANCE_POLICY) ?: ErrorTolerancePolicy.DEFAULT
        val hasErrors = TopDownAnalyzerFacadeForJSIR.checkForErrors(inputArtifact.sourceFiles, inputArtifact.bindingContext, errorPolicy)

        val dependencies = testServices.moduleDescriptorProvider.getModuleDescriptor(module).allDependencyModules.map { it as ModuleDescriptorImpl }
        val allDependencies = dependencies.map { testServices.jsLibraryProvider.getCompiledLibraryByDescriptor(it) }

        serializeModuleIntoKlib(
            configuration[CommonConfigurationKeys.MODULE_NAME]!!,
            project,
            configuration,
            configuration.get(IrMessageLogger.IR_MESSAGE_LOGGER) ?: IrMessageLogger.None,
            inputArtifact.bindingContext,
            inputArtifact.sourceFiles,
            klibPath = outputFile,
            allDependencies,
            inputArtifact.irModuleFragment,
            inputArtifact.expectDescriptorToSymbol,
            cleanFiles = emptyList(),
            nopack = true,
            perFile = false,
            containsErrorCode = hasErrors,
            abiVersion = KotlinAbiVersion.CURRENT, // TODO get from test file data
            jsOutputName = null
        )

        val lib = jsResolveLibraries(
            dependencies.map { testServices.jsLibraryProvider.getPathByDescriptor(it) } + listOf(outputFile),
            configuration[JSConfigurationKeys.REPOSITORIES] ?: emptyList(),
            configuration[IrMessageLogger.IR_MESSAGE_LOGGER].toResolverLogger()
        ).getFullResolvedList().last().library

        val moduleDescriptor = JsFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
            lib,
            configuration.languageVersionSettings,
            LockBasedStorageManager("ModulesStructure"),
            testServices.moduleDescriptorProvider.getModuleDescriptor(module).builtIns,
            packageAccessHandler = null,
            lookupTracker = LookupTracker.DO_NOTHING
        )
        moduleDescriptor.setDependencies(dependencies + moduleDescriptor)
        testServices.moduleDescriptorProvider.replaceModuleDescriptorForModule(module, moduleDescriptor)

        return BinaryArtifacts.Js.JsKlibArtifact(File(outputFile), moduleDescriptor, lib)
    }
}