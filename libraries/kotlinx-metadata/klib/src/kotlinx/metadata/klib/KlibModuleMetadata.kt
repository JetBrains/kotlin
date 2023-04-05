/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("DEPRECATION")

package kotlinx.metadata.klib

import kotlinx.metadata.KmAnnotation
import kotlinx.metadata.internal.common.KmModuleFragment
import kotlinx.metadata.internal.*
import kotlinx.metadata.klib.impl.*
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.library.metadata.parsePackageFragment
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.serialization.ApproximatingStringTable

/**
 * Allows to modify the way fragments of the single package are read by [KlibModuleMetadata.read].
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
    val annotations: List<KmAnnotation>
) {

    /**
     * Serialized representation of module metadata.
     */
    class SerializedKlibMetadata(
        val header: ByteArray,
        val fragments: List<List<ByteArray>>,
        val fragmentNames: List<String>
    )

    /**
     * Specifies access to library's metadata.
     */
    interface MetadataLibraryProvider {
        val moduleHeaderData: ByteArray
        fun packageMetadataParts(fqName: String): Set<String>
        fun packageMetadata(fqName: String, partName: String): ByteArray
    }

    companion object {
        /**
         * Deserializes metadata from the given [library].
         * @param readStrategy specifies the way module fragments of a single package are modified (e.g. merged) after deserialization.
         */
        fun read(
            library: MetadataLibraryProvider,
            readStrategy: KlibModuleFragmentReadStrategy = KlibModuleFragmentReadStrategy.DEFAULT
        ): KlibModuleMetadata {
            val moduleHeaderProto = parseModuleHeader(library.moduleHeaderData)
            val headerNameResolver = NameResolverImpl(moduleHeaderProto.strings, moduleHeaderProto.qualifiedNames)
            val moduleHeader = moduleHeaderProto.readHeader(headerNameResolver)
            val fileIndex = SourceFileIndexReadExtension(moduleHeader.file)
            val moduleFragments = moduleHeader.packageFragmentName.flatMap { packageFqName ->
                library.packageMetadataParts(packageFqName).map { part ->
                    val packageFragment = parsePackageFragment(library.packageMetadata(packageFqName, part))
                    val nameResolver = NameResolverImpl(packageFragment.strings, packageFragment.qualifiedNames)
                    KmModuleFragment().apply { packageFragment.accept(this, nameResolver, listOf(fileIndex)) }
                }.let(readStrategy::processModuleParts)
            }
            return KlibModuleMetadata(moduleHeader.moduleName, moduleFragments, moduleHeader.annotation)
        }
    }

    /**
     * Writes metadata back to serialized representation.
     * @param writeStrategy specifies the way module fragments are modified (e.g. split) before serialization.
     */
    fun write(
        writeStrategy: KlibModuleFragmentWriteStrategy = KlibModuleFragmentWriteStrategy.DEFAULT
    ): SerializedKlibMetadata {
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
            fragments.map {
                val c = WriteContext(ApproximatingStringTable(), listOf(reverseIndex))
                KlibModuleFragmentWriter(c.strings as ApproximatingStringTable, c.contextExtensions).also(it::accept).write()
            }
        }
        // This context and string table is only required for module-level annotations.
        val c = WriteContext(ApproximatingStringTable(), listOf(reverseIndex))
        return SerializedKlibMetadata(
            header.writeHeader(c).build().toByteArray(),
            groupedProtos.map { it.value.map(ProtoBuf.PackageFragment::toByteArray) },
            header.packageFragmentName
        )
    }
}

private fun KmModuleFragment.fqNameOrFail(): String =
    fqName ?: error("Module fragment must have a fully-qualified name.")

private fun KmModuleFragment.isEmpty(): Boolean =
    classes.isEmpty() && (pkg?.let { it.functions.isEmpty() && it.properties.isEmpty() && it.typeAliases.isEmpty() } ?: true)
