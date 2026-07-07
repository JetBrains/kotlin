/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import kotlinx.metadata.klib.KlibMetadataVersion
import kotlinx.metadata.klib.KlibModuleMetadata
import kotlinx.metadata.klib.fqName
import org.jetbrains.kotlin.backend.common.linkage.IrDeserializer.TopLevelSymbolKind
import org.jetbrains.kotlin.backend.common.serialization.CompatibilityMode
import org.jetbrains.kotlin.backend.common.serialization.IrModuleDeserializer
import org.jetbrains.kotlin.backend.common.serialization.IrModuleDeserializerKind
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.library.KLIB_PROPERTY_PACKAGE
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.components.metadata
import org.jetbrains.kotlin.library.metadata.KlibDeserializedContainerSource
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.library.metadataVersion
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.NativeStandardInteropNames
import org.jetbrains.kotlin.name.isOneSegmentFQN
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.utils.addToStdlib.getOrSetIfNull
import org.jetbrains.kotlin.utils.putToMultiMap
import java.lang.ref.SoftReference
import kotlin.metadata.ClassName
import kotlin.metadata.KmClass
import kotlin.metadata.KmClassifier
import kotlin.metadata.KmFunction
import kotlin.metadata.KmProperty

/**
 * IR deserializer for C-interop Klibs.
 *
 * Note that interop Klibs do not contain IR, only metadata. But because both are structurally quite similar, this deserializer is able to
 * read metadata and directly convert it to IR, without reaching out help of many other compiler subsystems like frontend or descriptors.
 *
 * It supports only those metadata constructs which are expected to be present in C-interop Klibs. However, in practice, it's almost all of
 * them (some notable exceptions are context parameters and classes with type parameters). So in theory, it is not far away from a
 * general-purpose metadata-to-IR deserializer/converter.
 *
 * It returns regular (non-lazy), body-less IR, with a top-level-class grauallity (i.e. even if one class member is referenced, it
 * deserializes the entire top-level class along with its nested classes).
 */
internal class KonanInteropModuleDeserializer(
        private val deserializationConfiguration: DeserializationConfiguration,
        moduleDescriptor: ModuleDescriptor,
        override val klib: KotlinLibrary,
        private val isLibraryCached: Boolean,
        private val linker: KonanIrLinker,
) : IrModuleDeserializer(moduleDescriptor, klib.versions.abiVersion ?: KotlinAbiVersion.CURRENT) {
    init {
        require(klib.isCInteropLibrary())
    }

    private val symbolTable = linker.symbolTable
    private val metadataReader = KlibMetadataReader(klib)
    private val moduleHeaderProto: KlibMetadataProtoBuf.Header by lazy { parseModuleHeader(klib.metadata.moduleHeaderData) }

    // Interop Klibs may declare only one package, and its FQ name is declared in the manifest.
    private val definedPackageFqName: FqName = klib.manifestProperties.getProperty(KLIB_PROPERTY_PACKAGE)?.let(::FqName)
            ?: error("Interop klib ${klib.path} does not contain an expected manifest property: $KLIB_PROPERTY_PACKAGE")
    override fun getDefinedPackageNames(): Set<FqName> = setOf(definedPackageFqName)

    override val kind get() = IrModuleDeserializerKind.DESERIALIZED
    override val moduleFragment: IrModuleFragment = IrModuleFragmentImpl(moduleDescriptor)
    private var externalIrPackageFragment: IrExternalPackageFragment? = null
    private var typeDefinitionsIrFile: IrFile? = null

    /** The cache of the declarations deserialized by the current deserializer. */
    private val declarationTracker = object : CInteropKlibMetadata2IRTransformer.DeclarationTracker() {
        override fun onNewClass(clazz: IrClass) {
            super.onNewClass(clazz)
            linker.fakeOverrideBuilder.enqueueClass(clazz, clazz.symbol.signature!!, CompatibilityMode.CURRENT)
        }
    }

    private val transformer = CInteropKlibMetadata2IRTransformer(
            symbolTable,
            symbols = CInteropKlibMetadata2IRTransformer.ExternalSymbols(symbolTable),
            declarationTracker = declarationTracker,
            getNestedKmClass = { classId -> metadataReader.retrieveDeclarationsById(classId.toDeclarationId(), removeMetadataRepresentation = true)?.first() as KmClass? },
            getOrCreateContainingPackageFragment = ::getOrCreateContainingPackageFragment,
            getReferencedDeclarationSymbol = { signature, kind -> linker.deserializeOrReturnUnboundIrSymbolIfPartialLinkageEnabled(signature, kind, this) },
            irProviderForLazyAnnotations = linker,
    )

    private fun IdSignature.isInteropSignature() = IdSignature.Flags.IS_NATIVE_INTEROP_LIBRARY.test()

    override fun contains(idSig: IdSignature): Boolean {
        if (!idSig.isInteropSignature()) {
            return false
        }

        val commonSignature = ((idSig as? IdSignature.AccessorSignature)?.propertySignature ?: idSig)
                as? IdSignature.CommonSignature ?: return false
        val topLevelSignature = commonSignature.topLevelSignature() as IdSignature.CommonSignature
        val packageFqName = topLevelSignature.packageFqName()
        if (packageFqName != definedPackageFqName) {
            return false
        }

        // First, check for the presence of a top-level class. We assume that if it exists, all its members should also exist.
        // Note: Along classes, C-interop Klibs also define type aliases. However, all types in IR and metadata already provide their
        // expanded representation, and type aliases are not otherwise useful in IR, so there is no need to deserialize them.
        val topLevelName = FqName(topLevelSignature.declarationFqName)
        val topLevelClassId = MetadataDeclarationId(TopLevelSymbolKind.CLASS_SYMBOL, packageFqName, topLevelName)
        if (topLevelClassId in metadataReader.getDeclaredDeclarationIds()) {
            return true
        }

        if (FqName(commonSignature.declarationFqName).isOneSegmentFQN()) {
            // If no top-level class is found for a given FQ name, there may also exist such a top-level function or property.
            // Unfortunately, for them, there is no quick way to tell if an interop Klib contains one with a given signature, because
            // metadata does not store IdSignatures. It's necessary to invoke the actual deserialization, which will compute the signatures
            // on the fly, then match against the requested one.
            // Note: The following check will return a false positive if the symbol for a given function or property is already bound.
            // If this would indeed happen, it's most likely because the symbol was deserialized by another instance of
            // KonanInteropModuleDeserializer, most likely by its contains() method. At this time we don't consider it a problem,
            // because we usually stop on the first module deserializer to return true, and don't call contains() afterwards.
            return tryDeserializeIrSymbol(idSig, BinarySymbolData.SymbolKind.FUNCTION_SYMBOL) != null ||
                    tryDeserializeIrSymbol(idSig, BinarySymbolData.SymbolKind.PROPERTY_SYMBOL) != null
        }
        return false
    }

    override fun tryDeserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol? {
        declarationTracker.deserializedDeclarations[idSig]?.let {
            // The signature may have been already deserialized, just return it.
            return it.symbol
        }

        var searchForSymbolKind = symbolKind
        var commonSig = idSig
        if (commonSig is IdSignature.AccessorSignature) {
            // When looking for a property's accessor, try to deserialize the property instead.
            // Doing so will, in turn, deserialize its accessors.
            commonSig = commonSig.propertySignature
            searchForSymbolKind = BinarySymbolData.SymbolKind.PROPERTY_SYMBOL
        }
        commonSig = commonSig as? IdSignature.CommonSignature ?: return null

        val declarationFqName = FqName(commonSig.declarationFqName)
        if (!declarationFqName.isOneSegmentFQN()) {
            // When looking for a class member, try to deserialize the top-most containing class instead.
            // Doing so will, in turn, deserialize everything declared inside that class (including nested classes, recursively).
            // If the sought declaration is indeed defined somewhere inside this class, it will be linked to `symbol` in the process.
            val topLevelClassSig = commonSig.topLevelSignature()
            tryDeserializeIrSymbol(topLevelClassSig, BinarySymbolData.SymbolKind.CLASS_SYMBOL)
        } else {
            val packageFqName = FqName(commonSig.packageFqName)
            val declarationKind = when (searchForSymbolKind) {
                BinarySymbolData.SymbolKind.CLASS_SYMBOL -> TopLevelSymbolKind.CLASS_SYMBOL
                BinarySymbolData.SymbolKind.FUNCTION_SYMBOL -> TopLevelSymbolKind.FUNCTION_SYMBOL
                BinarySymbolData.SymbolKind.PROPERTY_SYMBOL -> TopLevelSymbolKind.PROPERTY_SYMBOL
                BinarySymbolData.SymbolKind.CONSTRUCTOR_SYMBOL,
                BinarySymbolData.SymbolKind.ENUM_ENTRY_SYMBOL -> error("This declaration cannot be top-level: $searchForSymbolKind")
                else -> error("Symbol kind is unsupported by C-interop Klib: $searchForSymbolKind")
            }
            val id = MetadataDeclarationId(declarationKind, packageFqName, declarationFqName)

            val kmDeclarations = metadataReader.retrieveDeclarationsById(id, removeMetadataRepresentation = true)
            if (kmDeclarations != null) {
                for (kmDeclaration in kmDeclarations) {
                    when (kmDeclaration) {
                        is KmClass -> transformer.transformTopLevelClass(kmDeclaration)
                        is KmFunction -> transformer.transformTopLevelFunction(kmDeclaration)
                        is KmProperty -> transformer.transformTopLevelProperty(kmDeclaration)
                        else -> error(kmDeclaration.javaClass.name)
                    }
                }
            }
        }

        // If the deserialization process above found a declaration with the requested signature, it should store it in this map.
        declarationTracker.deserializedDeclarations[idSig]?.let {
            return it.symbol
        }

        if (!declarationFqName.isOneSegmentFQN()) {
            // If a member was not found inside a class, it may be because the signature actually refers to a fake override.
            // F/Os are not present in Klib and will be created later, but the symbol for it must be created here.
            when (symbolKind) {
                BinarySymbolData.SymbolKind.FUNCTION_SYMBOL -> return symbolTable.referenceSimpleFunction(idSig)
                BinarySymbolData.SymbolKind.PROPERTY_SYMBOL -> return symbolTable.referenceProperty(idSig)
                else -> {}
            }
        }

        return null
    }

    override fun deserializedSymbolNotFound(idSig: IdSignature): Nothing = error("No C-Interop symbol found for $idSig")

    fun deserializeAllCStructsAndEnums() {
        for (id in metadataReader.getDeclaredDeclarationIds()) {
            if (id.kind != TopLevelSymbolKind.CLASS_SYMBOL) continue

            // All C structs and enums are expected to be top-level classes.
            // Also, nested classes cannot be loaded here directly, as all class members should be loaded only when deserializing
            // their parent class.
            if (!id.relativeDeclarationName.isOneSegmentFQN()) continue

            val kmClass = metadataReader.retrieveDeclarationsById(id, removeMetadataRepresentation = false)
                    ?.firstOrNull() as? KmClass ?: continue
            if (kmClass.inheritsFromCStructOrEnum()) {
                // At first, pass removeMetadataRepresentation = false, because we only use the metadata class to check if it is a C struct or enum.
                // If it is, pass removeMetadataRepresentation = true, because we are going to actually deserialize it. This helps to ensure
                // we only deserialize a given class once.
                metadataReader.retrieveDeclarationsById(id, removeMetadataRepresentation = true)
                transformer.transformTopLevelClass(kmClass)
            }
        }
    }

    private fun KmClass.inheritsFromCStructOrEnum(): Boolean = supertypes.any {
        val classFqName = (it.classifier as? KmClassifier.Class)?.name ?: return@any false
        classFqName == "kotlinx/cinterop/CStructVar" || classFqName == "kotlinx/cinterop/CEnum"
    }

    private fun getOrCreateContainingPackageFragment(forKmDeclaration: Any): IrPackageFragment {
        val containerSource = KlibDeserializedContainerSource(klib, moduleHeaderProto, deserializationConfiguration, definedPackageFqName, null)
        val descriptor = DeserializedSecondStageInteropPackageDescriptor(moduleDescriptor, definedPackageFqName, containerSource)
        if (forKmDeclaration is KmClass && forKmDeclaration.inheritsFromCStructOrEnum() && !isLibraryCached) {
            // Most declarations from C-interop Klib are just stubs which shouldn't be lowered, so they are
            // put inside IrExternalPackageFragment, the same way as on the first stage of compilation.
            // But C structs and enums should be (unless already cached), so instead they are put in a special IrFile,
            // as only IrFiles participate in lowering.
            // If the interop Klib is cached, the cache should contain all the implementation already compiled to
            // binary code, so those classes may be treated as regular dependencies.
            return ::typeDefinitionsIrFile.getOrSetIfNull {
                val fileEntry = NaiveSourceBasedFileEntryImpl(NativeStandardInteropNames.cTypeDefinitionsFileName)
                val irFile = IrFileImpl(fileEntry, IrFileSymbolImpl(descriptor), definedPackageFqName, moduleFragment)
                moduleFragment.files += irFile
                irFile
            }
        } else {
            return ::externalIrPackageFragment.getOrSetIfNull {
                IrExternalPackageFragmentImpl(IrExternalPackageFragmentSymbolImpl(descriptor), definedPackageFqName)
            }
        }
    }

    private class KlibMetadataReader(
            private val klib: KotlinLibrary,
    ) {
        private var areDeclarationIdsLoaded = false

        // A cache of all package-level metadata declarations defined in a Klib. Note that this also includes nested classes,
        // because in metadata, they are serialized at package level. (In other words, this map contains both `Map` and `Map.Entry`, but not
        // their methods.)
        // The value is of type Any, because there is no common type between e.g., KmClass and KmFunction.
        // It is wrapped in a List, because there may be multiple functions and properites (although not classes) with the same FQ name.
        // It is wrapped in a SoftReference, because keeping all metadata from all referenced interop libraries may have a significant
        // pressure on memory. If the requested declaration is GC'ed, the entire Klib is loaded again.
        // Also, when a given metadata declaration is converted into an IR declaration, the entry is replaced with a `null` value, as its
        // metadata representation is no longer needed. The entry is not removed completely, so that the map still has keys for all the
        // defined declarations.
        val allMetadataDeclarations: MutableMap<MetadataDeclarationId, SoftReference<List<Any>>?> = mutableMapOf()

        private fun ensureDeclarationIdsLoaded() {
            // Metadata has to be loaded at least once to populate the IDs of available declarations.
            if (!areDeclarationIdsLoaded) {
                loadAndCacheMetadata()
                areDeclarationIdsLoaded = true
            }
        }

        private fun loadAndCacheMetadata(): Map<MetadataDeclarationId, List<Any>> {
            val metadataComponent = klib.metadata
            val provider = object : KlibModuleMetadata.MetadataLibraryProvider {
                override val moduleHeaderData get() = metadataComponent.moduleHeaderData
                override val metadataVersion = KlibMetadataVersion((klib.metadataVersion?.toArray()
                        ?: error("No metadata version specified in ${klib.path}")))

                override fun packageMetadata(fqName: String, partName: String) = metadataComponent.getPackageFragment(fqName, partName)
                override fun packageMetadataParts(fqName: String) = metadataComponent.getPackageFragmentNames(fqName)
            }
            val metadataModule = KlibModuleMetadata.readStrict(provider)

            val deserializedDeclarations = mutableMapOf<MetadataDeclarationId, MutableList<Any>>()
            for (packageFragment in metadataModule.fragments) {
                val packageFqName = FqName(packageFragment.fqName ?: continue)
                for (clazz in packageFragment.classes) {
                    val classFqName = clazz.name.declarationFqName
                    val id = MetadataDeclarationId(TopLevelSymbolKind.CLASS_SYMBOL, packageFqName, classFqName)
                    deserializedDeclarations.putToMultiMap(id, clazz)
                }

                val pkg = packageFragment.pkg ?: continue
                for (function in pkg.functions) {
                    val id = MetadataDeclarationId(TopLevelSymbolKind.FUNCTION_SYMBOL, packageFqName, FqName(function.name))
                    deserializedDeclarations.putToMultiMap(id, function)
                }
                for (property in pkg.properties) {
                    val id = MetadataDeclarationId(TopLevelSymbolKind.PROPERTY_SYMBOL, packageFqName, FqName(property.name))
                    deserializedDeclarations.putToMultiMap(id, property)
                }
            }

            for ([id, declarations] in deserializedDeclarations) {
                val existingRef = allMetadataDeclarations[id]
                val storeInCache = if (existingRef == null) {
                    if (id !in allMetadataDeclarations) {
                        // There is no entry, this is the first deserialization. Cache the results.
                        true
                    } else {
                        // There is a `null` entry. It means that the declaration with a given ID was already converted to IR.
                        // Don't store the metadata for this declaration again to avoid creating IR for the same declaration twice.
                        false
                    }
                } else {
                    if (existingRef.get() == null) {
                        // There is a SoftReference entry, but the referenced value has been GC'ed. Cache it again.
                        true
                    } else {
                        // There is a SoftReference entry and it still holds a value. It will be the same as the freshly deserialized
                        // declaration, so just keep the old one.
                        false
                    }
                }
                if (storeInCache) {
                    allMetadataDeclarations[id] = SoftReference(declarations)
                }
            }

            return deserializedDeclarations
        }

        fun getDeclaredDeclarationIds(): Set<MetadataDeclarationId> {
            ensureDeclarationIdsLoaded()
            return allMetadataDeclarations.keys
        }

        /**
         * @param removeMetadataRepresentation - If true, calling this function a second time for the same id will return `null`.
         */
        fun retrieveDeclarationsById(id: MetadataDeclarationId, removeMetadataRepresentation: Boolean): List<Any>? {
            ensureDeclarationIdsLoaded()
            val ref = if (removeMetadataRepresentation) {
                allMetadataDeclarations.replace(id, null)
            } else {
                allMetadataDeclarations[id]
            } ?: return null
            ref.get()?.let {
                return it
            }

            // If the cache got GC-ed, re-read the entire Klib.
            val allDeclarations = loadAndCacheMetadata()
            return allDeclarations[id]
        }
    }

    private data class MetadataDeclarationId(
            val kind: TopLevelSymbolKind,
            val packageFqName: FqName,
            val relativeDeclarationName: FqName,
    )

    companion object {
        private fun ClassId.toDeclarationId() = MetadataDeclarationId(
                kind = TopLevelSymbolKind.CLASS_SYMBOL,
                packageFqName = packageFqName,
                relativeDeclarationName = relativeClassName
        )
    }
}

private val ClassName.declarationFqName get() = FqName(substringAfterLast('/'))

class DeserializedSecondStageInteropPackageDescriptor(
        module: ModuleDescriptor,
        fqName: FqName,
        private val containerSource: KlibDeserializedContainerSource,
) : PackageFragmentDescriptorImpl(module, fqName) {
    override fun getMemberScope(): MemberScope = error("K1-specific functionality is not supported.")
    override fun getSource(): SourceElement = containerSource
}