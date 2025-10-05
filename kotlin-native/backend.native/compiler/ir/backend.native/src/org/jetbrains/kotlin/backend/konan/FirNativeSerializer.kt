package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.serialization.IrSerializationSettings
import org.jetbrains.kotlin.backend.common.serialization.serializeModuleIntoKlib
import org.jetbrains.kotlin.backend.konan.driver.LightPhaseContext
import org.jetbrains.kotlin.backend.konan.driver.phases.Fir2IrOutput
import org.jetbrains.kotlin.backend.konan.driver.phases.FirOutput
import org.jetbrains.kotlin.backend.konan.driver.phases.FirSerializerInput
import org.jetbrains.kotlin.backend.konan.driver.phases.SerializerOutput
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrModuleSerializer
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.fir.reportToMessageCollector
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.pipeline.Fir2KlibMetadataSerializer
import org.jetbrains.kotlin.fir.pipeline.FirResult
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.library.metadata.resolver.TopologicalLibraryOrder

internal fun LightPhaseContext.firSerializer(input: FirOutput): SerializerOutput? = when (input) {
    !is FirOutput.Full -> null
    else -> firSerializerBase(input.firResult, null)
}

internal fun LightPhaseContext.fir2IrSerializer(input: FirSerializerInput): SerializerOutput {
    return firSerializerBase(input.firToIrOutput.firResult, input.firToIrOutput, produceHeaderKlib = input.produceHeaderKlib)
}

internal fun LightPhaseContext.firSerializerBase(
        firResult: FirResult,
        fir2IrOutput: Fir2IrOutput?,
        produceHeaderKlib: Boolean = false,
): SerializerOutput {
    val configuration = config.configuration
    val usedResolvedLibraries = fir2IrOutput?.let {
        config.resolvedLibraries.getFullResolvedList(TopologicalLibraryOrder).filter {
            (!it.isDefault && !configuration.getBoolean(KonanConfigKeys.PURGE_USER_LIBS)) || it in fir2IrOutput.usedLibraries
        }
    }

    val irModuleFragment = fir2IrOutput?.fir2irActualizedResult?.irModuleFragment
    val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
    val diagnosticReporter = DiagnosticReporterFactory.createPendingReporter(messageCollector)
    val serializerOutput = serializeModuleIntoKlib(
            moduleName = irModuleFragment?.name?.asString() ?: firResult.outputs.last().session.moduleData.name.asString(),
            irModuleFragment = irModuleFragment,
            configuration = configuration,
            diagnosticReporter = diagnosticReporter,
            metadataSerializer = Fir2KlibMetadataSerializer(
                    configuration,
                    firResult.outputs,
                    fir2IrOutput?.fir2irActualizedResult,
                    exportKDoc = shouldExportKDoc(),
                    produceHeaderKlib = produceHeaderKlib,
            ),
            cleanFiles = emptyList(),
            dependencies = usedResolvedLibraries?.map { it.library as KonanLibrary }.orEmpty(),
            createModuleSerializer = { irDiagnosticReporter ->
                KonanIrModuleSerializer(
                    settings = IrSerializationSettings(
                        configuration = configuration,
                        publicAbiOnly = produceHeaderKlib,
                    ),
                    diagnosticReporter = irDiagnosticReporter,
                    irBuiltIns = fir2IrOutput?.fir2irActualizedResult?.irBuiltIns!!,
                )
            },
    )
    val renderDiagnosticNames = configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
    diagnosticReporter.reportToMessageCollector(messageCollector, renderDiagnosticNames)
    if (diagnosticReporter.hasErrors) {
        throw KonanCompilationException("Compilation failed: there were errors during module serialization")
    }
    return serializerOutput
}
