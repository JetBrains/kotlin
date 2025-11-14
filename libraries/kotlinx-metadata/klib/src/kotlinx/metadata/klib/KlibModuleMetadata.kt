/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.klib

import kotlinx.metadata.klib.impl.*
import kotlinx.metadata.klib.impl.readHeader
import kotlinx.metadata.klib.impl.writeHeader
import kotlin.metadata.KmAnnotation
import kotlin.metadata.internal.common.KmModuleFragment
import kotlin.metadata.internal.*
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.library.metadata.parsePackageFragment
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.serialization.ApproximatingStringTable

/**
 * Allows to modify the way fragments of the single package are read by [KlibModuleMetadata.readStrict] and [KlibModuleMetadata.readLenient].
 * For example, it may be convenient to join fragments into a single one.
 */
interface KlibModuleFragmentReadStrategy {
    fun processModuleParts(parts: List<KmModuleFragment>): List<KmModuleFragment>

    companion object {
        val DEFAULT = object : KlibModuleFragmentReadStrategy {
            override fun processModuleParts(parts: List<KmModuleFragment>) =
                parts
        }
    }
}

/**
 * Allows to modify the way module fragments are written by [KlibModuleMetadata.write].
 * For example, splitting big fragments into several small one allows to improve IDE performance.
 */
interface KlibModuleFragmentWriteStrategy {
    fun processPackageParts(parts: List<KmModuleFragment>): List<KmModuleFragment>

    companion object {
        val DEFAULT = object : KlibModuleFragmentWriteStrategy {
            override fun processPackageParts(parts: List<KmModuleFragment>): List<KmModuleFragment> =
                parts
        }
    }
}

/**
 * Represents the parsed metadata of KLIB.
 */
class KlibModuleMetadata(
    val name: String,
    val fragments: List<KmModuleFragment>,
    val annotations: List<KmAnnotation>,
    val metadataVersion: KlibMetadataVersion,
    internal val isAllowedToWrite: Boolean = true,
) {
    /**
     * Serialized representation of module metadata.
     */
    class SerializedKlibMetadata(
        val header: ByteArray,
        val fragments: List<List<ByteArray>>,
        val fragmentNames: List<String>,
        val metadataVersion: KlibMetadataVersion,
    )

    /**
     * Specifies access to library's metadata.
     */
    interface MetadataLibraryProvider {
        val moduleHeaderData: ByteArray
        val metadataVersion: KlibMetadataVersion
        fun packageMetadataParts(fqName: String): Set<String>
        fun packageMetadata(fqName: String, partName: String): ByteArray
    }

    companion object {
        /**
         * Deserializes metadata from the given [library].
         * This method is strict by default. Prefer calling [readStrict] or [readLenient] explicitly.
         *
         * @param readStrategy specifies the way module fragments of a single package are modified (e.g. merged) after deserialization.
         */
        @Deprecated(level = DeprecationLevel.ERROR, message = "Use readStrict or readLenient instead")
        fun read(
            library: MetadataLibraryProvider,
            readStrategy: KlibModuleFragmentReadStrategy = KlibModuleFragmentReadStrategy.DEFAULT
        ): KlibModuleMetadata = readStrict(library, readStrategy)

        /**
         * Deserializes metadata from the given [library].
         *
         * This method can read only supported metadata versions (see [KlibMetadataVersion.LATEST_STABLE_SUPPORTED]).
         * It will throw an exception if the metadata version is greater than what kotlinx-metadata-klib understands.
         *
         * @param readStrategy specifies the way module fragments of a single package are modified (e.g. merged) after deserialization.
         */
        fun readStrict(
            library: MetadataLibraryProvider,
            readStrategy: KlibModuleFragmentReadStrategy = KlibModuleFragmentReadStrategy.DEFAULT,
        ): KlibModuleMetadata = readImpl(library, readStrategy, lenient = false)

        /**
         * Deserializes metadata from the given [library]
         * This method makes best effort to read unsupported metadata versions.
         * [KlibModuleMetadata] instances obtained from this method cannot be written.
         *
         * @param readStrategy specifies the way module fragments of a single package are modified (e.g. merged) after deserialization.
         */
        fun readLenient(
            library: MetadataLibraryProvider,
            readStrategy: KlibModuleFragmentReadStrategy = KlibModuleFragmentReadStrategy.DEFAULT,
        ): KlibModuleMetadata = readImpl(library, readStrategy, lenient = true)

        private fun readImpl(
            library: MetadataLibraryProvider,
            readStrategy: KlibModuleFragmentReadStrategy = KlibModuleFragmentReadStrategy.DEFAULT,
            lenient: Boolean,
        ): KlibModuleMetadata {
            checkMetadataVersionForRead(library.metadataVersion, lenient)

            val moduleHeaderProto = parseModuleHeader(library.moduleHeaderData)
            val headerNameResolver = NameResolverImpl(moduleHeaderProto.strings, moduleHeaderProto.qualifiedNames)
            val moduleHeader = moduleHeaderProto.readHeader(headerNameResolver)
            val fileIndex = SourceFileIndexReadExtension(moduleHeader.file)
            val moduleFragments = moduleHeader.packageFragmentName.flatMap { packageFqName ->
                library.packageMetadataParts(packageFqName).map { part ->
                    val packageFragment = parsePackageFragment(library.packageMetadata(packageFqName, part))
                    val nameResolver = NameResolverImpl(packageFragment.strings, packageFragment.qualifiedNames)
                    packageFragment.toKmModuleFragment(nameResolver, listOf(fileIndex))
                }.let(readStrategy::processModuleParts)
            }
            return KlibModuleMetadata(
                moduleHeader.moduleName,
                moduleFragments,
                moduleHeader.annotation,
                library.metadataVersion,
                isAllowedToWrite = !lenient,
            )
        }

        private fun checkMetadataVersionForRead(klibMetadataVersion: KlibMetadataVersion, lenient: Boolean) {
            if (lenient) return
            val metadataVersion = MetadataVersion(klibMetadataVersion.toArray(), isStrictSemantics = false)
            if (!metadataVersion.isCompatibleWithCurrentCompilerVersion()) {
                error("Provided metadata instance has version $metadataVersion, while maximum supported version is ${MetadataVersion.INSTANCE_NEXT}. To support newer versions, update the kotlinx-metadata-klib library.")
            }
        }
    }

    /**
     * Writes metadata back to serialized representation.
     * @param writeStrategy specifies the way module fragments are modified (e.g. split) before serialization.
     */
    fun write(
        writeStrategy: KlibModuleFragmentWriteStrategy = KlibModuleFragmentWriteStrategy.DEFAULT
    ): SerializedKlibMetadata {
        if (!isAllowedToWrite) {
            error("Metadata read in lenient mode cannot be written back")
        }

        val reverseIndex = ReverseSourceFileIndexWriteExtension()

        val groupedFragments = fragments
            .groupBy(KmModuleFragment::fqNameOrFail)
            .mapValues { writeStrategy.processPackageParts(it.value) }

        val header = KlibHeader(
            name,
            reverseIndex.fileIndex,
            groupedFragments.map { it.key },
            groupedFragments.filter { it.value.all(KmModuleFragment::isEmpty) }.map { it.key },
            annotations
        )
        val groupedProtos = groupedFragments.mapValues { (_, fragments) ->
            fragments.map { mf ->
                val c = WriteContext(ApproximatingStringTable(), listOf(reverseIndex))
                KlibModuleFragmentWriter(c.strings as ApproximatingStringTable, c.contextExtensions).also { it.writeModuleFragment(mf) }.write()
            }
        }
        // This context and string table is only required for module-level annotations.
        val c = WriteContext(ApproximatingStringTable(), listOf(reverseIndex))
        return SerializedKlibMetadata(
            header.writeHeader(c).build().toByteArray(),
            groupedProtos.map { it.value.map(ProtoBuf.PackageFragment::toByteArray) },
            header.packageFragmentName,
            metadataVersion,
        )
    }
}

private fun KmModuleFragment.fqNameOrFail(): String =
    fqName ?: error("Module fragment must have a fully-qualified name.")

private fun KmModuleFragment.isEmpty(): Boolean =
    classes.isEmpty() && (pkg?.let { it.functions.isEmpty() && it.properties.isEmpty() && it.typeAliases.isEmpty() } ?: true)
