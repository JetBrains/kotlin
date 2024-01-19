package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.backend.common.serialization.CompatibilityMode
import org.jetbrains.kotlin.backend.common.serialization.serializeModuleIntoKlib
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.driver.phases.Fir2IrOutput
import org.jetbrains.kotlin.backend.konan.driver.phases.FirOutput
import org.jetbrains.kotlin.backend.konan.driver.phases.FirSerializerInput
import org.jetbrains.kotlin.backend.konan.driver.phases.SerializerOutput
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrModuleSerializer
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.fir.reportToMessageCollector
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.ConstValueProviderImpl
import org.jetbrains.kotlin.fir.backend.extractFirDeclarations
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.pipeline.FirResult
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.serialization.FirKLibSerializerExtension
import org.jetbrains.kotlin.fir.serialization.serializeSingleFirFile
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.library.metadata.resolver.TopologicalLibraryOrder
import org.jetbrains.kotlin.utils.toMetadataVersion

internal fun PhaseContext.firSerializer(input: FirOutput): SerializerOutput? = when (input) {
    !is FirOutput.Full -> null
    else -> firSerializerBase(input.firResult, null)
}

internal fun PhaseContext.fir2IrSerializer(input: FirSerializerInput): SerializerOutput {
    return firSerializerBase(input.firToIrOutput.firResult, input.firToIrOutput, produceHeaderKlib = input.produceHeaderKlib)
}

internal fun PhaseContext.firSerializerBase(
        firResult: FirResult,
        fir2IrOutput: Fir2IrOutput?,
        produceHeaderKlib: Boolean = false,
): SerializerOutput {
    val configuration = config.configuration
    val sourceFiles = mutableListOf<KtSourceFile>()
    val firFilesAndSessionsBySourceFile = mutableMapOf<KtSourceFile, Triple<FirFile, FirSession, ScopeSession>>()

    for (firOutput in firResult.outputs) {
        for (firFile in firOutput.fir) {
            sourceFiles.add(firFile.sourceFile!!)
            firFilesAndSessionsBySourceFile[firFile.sourceFile!!] = Triple(firFile, firOutput.session, firOutput.scopeSession)
        }
    }

    val metadataVersion =
            configuration.get(CommonConfigurationKeys.METADATA_VERSION)
                    ?: configuration.languageVersionSettings.languageVersion.toMetadataVersion()

    val usedResolvedLibraries = fir2IrOutput?.let {
        config.resolvedLibraries.getFullResolvedList(TopologicalLibraryOrder).filter {
            (!it.isDefault && !configuration.getBoolean(KonanConfigKeys.PURGE_USER_LIBS)) || it in fir2IrOutput.usedLibraries
        }
    }

    val actualizedFirDeclarations = fir2IrOutput?.irActualizedResult?.actualizedExpectDeclarations?.extractFirDeclarations()
    val diagnosticReporter = DiagnosticReporterFactory.createPendingReporter()
    val serializerOutput = serializeModuleIntoKlib(
            moduleName = fir2IrOutput?.irModuleFragment?.descriptor?.name?.asString()
                    ?: firResult.outputs.last().session.moduleData.name.asString(),
            irModuleFragment = fir2IrOutput?.irModuleFragment,
            configuration = configuration,
            diagnosticReporter = diagnosticReporter,
            sourceFiles = sourceFiles,
            compatibilityMode = CompatibilityMode.CURRENT,
            cleanFiles = emptyList(),
            dependencies = usedResolvedLibraries?.map { it.library as KonanLibrary }.orEmpty(),
            createModuleSerializer = { irDiagnosticReporter,
                                       irBuiltIns,
                                       compatibilityMode,
                                       normalizeAbsolutePaths,
                                       sourceBaseDirs,
                                       languageVersionSettings,
                                       shouldCheckSignaturesOnUniqueness ->
                KonanIrModuleSerializer(
                        diagnosticReporter = irDiagnosticReporter,
                        irBuiltIns = irBuiltIns,
                        compatibilityMode = compatibilityMode,
                        normalizeAbsolutePaths = normalizeAbsolutePaths,
                        sourceBaseDirs = sourceBaseDirs,
                        languageVersionSettings = languageVersionSettings,
                        bodiesOnlyForInlines = produceHeaderKlib,
                        skipPrivateApi = produceHeaderKlib,
                        shouldCheckSignaturesOnUniqueness = shouldCheckSignaturesOnUniqueness,
                )
            },
            serializeFileMetadata = { ktSourceFile ->
                val (firFile, originalSession, originalScopeSession) = firFilesAndSessionsBySourceFile[ktSourceFile]
                        ?: error("cannot find FIR file by source file ${ktSourceFile.name} (${ktSourceFile.path})")
                val session = fir2IrOutput?.components?.session ?: originalSession
                val scopeSession = fir2IrOutput?.components?.scopeSession ?: originalScopeSession
                val firProvider = fir2IrOutput?.components?.firProvider ?: originalSession.firProvider
                serializeSingleFirFile(
                        firFile,
                        session,
                        scopeSession,
                        actualizedFirDeclarations,
                        FirKLibSerializerExtension(
                                session, firProvider, metadataVersion,
                                fir2IrOutput?.let {
                                    ConstValueProviderImpl(fir2IrOutput.components)
                                },
                                allowErrorTypes = false,
                                exportKDoc = shouldExportKDoc(),
                                additionalMetadataProvider = fir2IrOutput?.components?.annotationsFromPluginRegistrar?.createAdditionalMetadataProvider()
                        ),
                        configuration.languageVersionSettings,
                        produceHeaderKlib,
                ) to firFile.packageFqName
            },
    )
    val renderDiagnosticNames = configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
    diagnosticReporter.reportToMessageCollector(messageCollector, renderDiagnosticNames)
    if (diagnosticReporter.hasErrors) {
        throw KonanCompilationException("Compilation failed: there were errors during module serialization")
    }
    return serializerOutput
}
