/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import org.jetbrains.kotlin.backend.common.klibAbiVersionForManifest
import org.jetbrains.kotlin.backend.common.serialization.IrSerializationSettings
import org.jetbrains.kotlin.backend.common.serialization.SerializerOutput
import org.jetbrains.kotlin.backend.common.serialization.serializeModuleIntoKlib
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrModuleSerializer
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.konan.library.writer.legacyNativeDependenciesInManifest
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.library.writer.KlibWriter
import org.jetbrains.kotlin.library.writer.includeIr
import org.jetbrains.kotlin.library.writer.includeMetadata
import org.jetbrains.kotlin.test.backend.handlers.AbstractKlibAbiDumpBeforeInliningSavingHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.configuration.nativeEnvironmentConfigurator
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.klibMetadataVersionOrDefault
import org.jetbrains.kotlin.util.tryMeasurePhaseTime
import java.io.File

class FirNativeKlibAbiDumpBeforeInliningSavingHandler(
    testServices: TestServices,
) : AbstractKlibAbiDumpBeforeInliningSavingHandler(testServices) {
    override fun serializeModule(module: TestModule, inputArtifact: IrBackendInput): BinaryArtifacts.KLib {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val diagnosticReporter = DiagnosticsCollectorImpl()
        val irDiagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(
            diagnosticReporter,
            configuration.languageVersionSettings
        )
        val outputFile = getAbiCheckKlibArtifactFile(module.name)

        serializeBare(module, inputArtifact, outputFile, configuration, irDiagnosticReporter)

        return BinaryArtifacts.KLib(outputFile, diagnosticReporter)
    }

    private fun serializeBare(
        module: TestModule,
        inputArtifact: IrBackendInput,
        outputKlibArtifactFile: File,
        configuration: CompilerConfiguration,
        diagnosticReporter: IrDiagnosticReporter,
    ) {
        require(inputArtifact is IrBackendInput.NativeAfterFrontendBackendInput) {
            "${this::class.java.simpleName} expects IrBackendInput.NativeAfterFrontendBackendInput as input"
        }

        val dependencies = inputArtifact.usedLibrariesForManifest
        val serializerOutput = serialize(configuration, dependencies, inputArtifact, diagnosticReporter)

        KlibWriter {
            manifest {
                moduleName(configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME))
                versions(
                    KotlinLibraryVersioning(
                        compilerVersion = KotlinCompilerVersion.getVersion(),
                        abiVersion = configuration.klibAbiVersionForManifest(),
                        metadataVersion = configuration.klibMetadataVersionOrDefault(),
                    )
                )
                platformAndTargets(BuiltInsPlatform.NATIVE, testServices.nativeEnvironmentConfigurator.getNativeTarget(module).name)
                legacyNativeDependenciesInManifest(serializerOutput.neededLibraries.map { it.uniqueName })
            }
            includeMetadata(serializerOutput.serializedMetadata ?: testServices.assertions.fail { "expected serialized metadata" })
            includeIr(serializerOutput.serializedIr)
        }.writeTo(outputKlibArtifactFile.path)
    }

    private fun serialize(
        configuration: CompilerConfiguration,
        usedLibrariesForManifest: List<KotlinLibrary>,
        inputArtifact: IrBackendInput.NativeAfterFrontendBackendInput,
        diagnosticReporter: IrDiagnosticReporter,
    ): SerializerOutput = configuration.perfManager.tryMeasurePhaseTime(PhaseType.IrSerialization) {
        serializeModuleIntoKlib(
            moduleName = inputArtifact.irModuleFragment.name.asString(),
            inputArtifact.irModuleFragment,
            configuration,
            diagnosticReporter,
            cleanFiles = emptyList(),
            usedLibrariesForManifest,
            createModuleSerializer = { irDiagnosticReporter ->
                KonanIrModuleSerializer(
                    settings = IrSerializationSettings(configuration),
                    diagnosticReporter = irDiagnosticReporter,
                    irBuiltIns = inputArtifact.irBuiltIns,
                )
            },
            inputArtifact.metadataSerializer ?: error("expected metadata serializer"),
        )
    }
}
