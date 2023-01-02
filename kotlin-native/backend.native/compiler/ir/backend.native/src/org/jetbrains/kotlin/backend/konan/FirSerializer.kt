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
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.serialization.*
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.library.SerializedIrFile
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.resolver.TopologicalLibraryOrder
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.utils.toMetadataVersion
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.serialization.MutableVersionRequirementTable
import org.jetbrains.kotlin.psi

internal fun PhaseContext.firSerializer(
        input: Fir2IrOutput
): SerializerOutput {
    val configuration = config.configuration
    val sourceFiles = input.firFiles.mapNotNull { it.sourceFile }
    val firFilesBySourceFile = input.firFiles.associateBy { it.sourceFile }
    val metadataVersion =
            configuration.get(CommonConfigurationKeys.METADATA_VERSION)
                    ?: configuration.languageVersionSettings.languageVersion.toMetadataVersion()

    val resolvedLibraries = config.resolvedLibraries.getFullResolvedList(TopologicalLibraryOrder) // FIXME KT-55603
    return serializeNativeModule(
            configuration = configuration,
            messageLogger = configuration.get(IrMessageLogger.IR_MESSAGE_LOGGER) ?: IrMessageLogger.None,
            sourceFiles,
            resolvedLibraries.map { it.library as KonanLibrary },
            input.fir2irResult.irModuleFragment,
            expectDescriptorToSymbol = mutableMapOf() // TODO: expect -> actual mapping
    ) { file ->
        val firFile = firFilesBySourceFile[file] ?: error("cannot find FIR file by source file ${file.name} (${file.path})")
        serializeSingleFirFile(
                firFile,
                input.session,
                input.scopeSession,
                FirNativeKLibSerializerExtension(input.session, metadataVersion, FirElementAwareSerializableStringTable()),
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

class FirNativeKLibSerializerExtension(
        override val session: FirSession,
        override val metadataVersion: BinaryVersion,
        override val stringTable: FirElementAwareSerializableStringTable
) : FirKLibSerializerExtension(session, metadataVersion, stringTable) {
    private fun declarationFileId(declaration: FirMemberDeclaration): Int? {
        val fileName = declaration.source.psi?.containingFile?.name ?: return null
        return stringTable.getStringIndex(fileName)
    }

    override fun serializeFunction(
            function: FirFunction,
            proto: ProtoBuf.Function.Builder,
            versionRequirementTable: MutableVersionRequirementTable?,
            childSerializer: FirElementSerializer
    ) {
        // inspired by KlibMetadataSerializerExtension.serializeFunction
        declarationFileId(function)?.let { proto.setExtension(KlibMetadataProtoBuf.functionFile, it) }
        function.annotations.forEach {
            proto.addExtension(KlibMetadataProtoBuf.functionAnnotation, annotationSerializer.serializeAnnotation(it))
        }
        function.receiverParameter?.annotations?.forEach {
            proto.addExtension(KlibMetadataProtoBuf.functionExtensionReceiverAnnotation, annotationSerializer.serializeAnnotation(it))
        }
        // TODO KT-56090 Serialize KDocString
        super.serializeFunction(function, proto, versionRequirementTable, childSerializer)
    }

    override fun serializeValueParameter(parameter: FirValueParameter, proto: ProtoBuf.ValueParameter.Builder) {
        parameter.annotations.forEach {
            proto.addExtension(KlibMetadataProtoBuf.parameterAnnotation, annotationSerializer.serializeAnnotation(it))
        }
        super.serializeValueParameter(parameter, proto)
    }

    override fun serializeProperty(
            property: FirProperty,
            proto: ProtoBuf.Property.Builder,
            versionRequirementTable: MutableVersionRequirementTable?,
            childSerializer: FirElementSerializer
    ) {
        // inspired by KlibMetadataSerializerExtension.serializeProperty
        declarationFileId(property)?.let { proto.setExtension(KlibMetadataProtoBuf.propertyFile, it) }
        property.annotations.forEach {
            proto.addExtension(KlibMetadataProtoBuf.propertyAnnotation, annotationSerializer.serializeAnnotation(it))
        }
        // TODO KT-56090 Serialize KDocString
        super.serializeProperty(property, proto, versionRequirementTable, childSerializer)
    }

    override fun serializeClass(
            klass: FirClass,
            proto: ProtoBuf.Class.Builder,
            versionRequirementTable: MutableVersionRequirementTable,
            childSerializer: FirElementSerializer
    ) {
        klass.annotations.forEach {
            proto.addExtension(KlibMetadataProtoBuf.classAnnotation, annotationSerializer.serializeAnnotation(it))
        }
        // TODO KT-56090 Serialize KDocString
        super.serializeClass(klass, proto, versionRequirementTable, childSerializer)
    }

    override fun serializeConstructor(
            constructor: FirConstructor,
            proto: ProtoBuf.Constructor.Builder,
            childSerializer: FirElementSerializer
    ) {
        constructor.annotations.forEach {
            proto.addExtension(KlibMetadataProtoBuf.constructorAnnotation, annotationSerializer.serializeAnnotation(it))
        }
        // TODO KT-56090 Serialize KDocString
        super.serializeConstructor(constructor, proto, childSerializer)
    }

    override fun serializeEnumEntry(enumEntry: FirEnumEntry, proto: ProtoBuf.EnumEntry.Builder) {
        enumEntry.annotations.forEach {
            proto.addExtension(KlibMetadataProtoBuf.enumEntryAnnotation, annotationSerializer.serializeAnnotation(it))
        }
        super.serializeEnumEntry(enumEntry, proto)
    }

    override fun serializeTypeAnnotation(annotation: FirAnnotation, proto: ProtoBuf.Type.Builder) {
        annotation.annotations.forEach {
            proto.addExtension(KlibMetadataProtoBuf.typeAnnotation, annotationSerializer.serializeAnnotation(it))
        }
        super.serializeTypeAnnotation(annotation, proto)
    }

    override fun serializeTypeParameter(typeParameter: FirTypeParameter, proto: ProtoBuf.TypeParameter.Builder) {
        typeParameter.annotations.forEach {
            proto.addExtension(KlibMetadataProtoBuf.typeParameterAnnotation, annotationSerializer.serializeAnnotation(it))
        }
        super.serializeTypeParameter(typeParameter, proto)
    }

}
