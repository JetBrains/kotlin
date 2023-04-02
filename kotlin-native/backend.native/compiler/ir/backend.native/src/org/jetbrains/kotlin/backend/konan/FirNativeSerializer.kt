package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.backend.common.serialization.CompatibilityMode
import org.jetbrains.kotlin.backend.common.serialization.metadata.makeSerializedKlibMetadata
import org.jetbrains.kotlin.backend.common.serialization.metadata.serializeKlibHeader
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.driver.phases.Fir2IrOutput
import org.jetbrains.kotlin.backend.konan.driver.phases.SerializerOutput
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrModuleSerializer
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.ConstValueProviderImpl
import org.jetbrains.kotlin.fir.backend.extractFirDeclarations
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.serialization.*
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.library.SerializedIrFile
import org.jetbrains.kotlin.library.metadata.resolver.TopologicalLibraryOrder
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.utils.toMetadataVersion

internal fun PhaseContext.firSerializer(
        input: Fir2IrOutput
): SerializerOutput {
    val configuration = config.configuration
    val sourceFiles = mutableListOf<KtSourceFile>()
    val firFilesAndSessionsBySourceFile = mutableMapOf<KtSourceFile, Triple<FirFile, FirSession, ScopeSession>>()

    for (firOutput in input.firResult.outputs) {
        for (firFile in firOutput.fir) {
            sourceFiles.add(firFile.sourceFile!!)
            firFilesAndSessionsBySourceFile[firFile.sourceFile!!] = Triple(firFile, firOutput.session, firOutput.scopeSession)
        }
    }

    val metadataVersion =
            configuration.get(CommonConfigurationKeys.METADATA_VERSION)
                    ?: configuration.languageVersionSettings.languageVersion.toMetadataVersion()

    val resolvedLibraries = config.resolvedLibraries.getFullResolvedList(TopologicalLibraryOrder)
    val usedResolvedLibraries = resolvedLibraries.filter {
        (!it.isDefault && !configuration.getBoolean(KonanConfigKeys.PURGE_USER_LIBS)) || it in input.usedLibraries
    }
    val actualizedFirDeclarations = input.irActualizationResult.extractFirDeclarations()
    return serializeNativeModule(
            configuration = configuration,
            messageLogger = configuration.get(IrMessageLogger.IR_MESSAGE_LOGGER) ?: IrMessageLogger.None,
            sourceFiles,
            usedResolvedLibraries.map { it.library as KonanLibrary },
            input.irModuleFragment,
            expectDescriptorToSymbol = mutableMapOf() // TODO: expect -> actual mapping
    ) { file ->
        val (firFile, session, scopeSession) = firFilesAndSessionsBySourceFile[file]
                ?: error("cannot find FIR file by source file ${file.name} (${file.path})")
        serializeSingleFirFile(
                firFile,
                session,
                scopeSession,
                actualizedFirDeclarations,
                FirKLibSerializerExtension(
                        session, metadataVersion,
                        ConstValueProviderImpl(input.components),
                        allowErrorTypes = false, exportKDoc = false
                ),
                configuration.languageVersionSettings,
        )
    }
}

class KotlinFileSerializedData(val metadata: ByteArray, val irData: SerializedIrFile)

internal fun PhaseContext.serializeNativeModule(
        configuration: CompilerConfiguration,
        messageLogger: IrMessageLogger,
        files: List<KtSourceFile>,
        dependencies: List<KonanLibrary>,
        moduleFragment: IrModuleFragment,
        expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>,
        serializeSingleFile: (KtSourceFile) -> ProtoBuf.PackageFragment
): SerializerOutput {
    assert(files.size == moduleFragment.files.size)

    val sourceBaseDirs = configuration[CommonConfigurationKeys.KLIB_RELATIVE_PATH_BASES] ?: emptyList()
    val absolutePathNormalization = configuration[CommonConfigurationKeys.KLIB_NORMALIZE_ABSOLUTE_PATH] ?: false
    val expectActualLinker = config.configuration.get(CommonConfigurationKeys.EXPECT_ACTUAL_LINKER) ?: false

    val serializedIr =
            KonanIrModuleSerializer(
                    messageLogger,
                    moduleFragment.irBuiltins,
                    expectDescriptorToSymbol,
                    skipExpects = !expectActualLinker,
                    CompatibilityMode.CURRENT,
                    normalizeAbsolutePaths = absolutePathNormalization,
                    sourceBaseDirs = sourceBaseDirs,
                    languageVersionSettings = configuration.languageVersionSettings,
            ).serializedIrModule(moduleFragment)

    val moduleDescriptor = moduleFragment.descriptor

    val compiledKotlinFiles = files.zip(serializedIr.files).map { (ktSourceFile, binaryFile) ->
        assert(ktSourceFile.path == binaryFile.path) {
            """The Kt and Ir files are put in different order
                Kt: ${ktSourceFile.path}
                Ir: ${binaryFile.path}
            """.trimMargin()
        }
        val packageFragment = serializeSingleFile(ktSourceFile)
        KotlinFileSerializedData(packageFragment.toByteArray(), binaryFile)
    }

    val header = serializeKlibHeader(
            configuration.languageVersionSettings, moduleDescriptor,
            compiledKotlinFiles.map { it.irData.fqName }.distinct().sorted(),
            emptyList()
    ).toByteArray()

    val serializedMetadata =
            makeSerializedKlibMetadata(
                    compiledKotlinFiles.groupBy { it.irData.fqName }
                            .map { (fqn, data) -> fqn to data.sortedBy { it.irData.path }.map { it.metadata } }.toMap(),
                    header
            )

    return SerializerOutput(serializedMetadata, serializedIr, null, dependencies)
}
