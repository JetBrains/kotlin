/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress(
    "MemberVisibilityCanBePrivate",
    "DEPRECATION",
    "INVISIBLE_MEMBER", // InconsistentKotlinMetadataException
    "INVISIBLE_REFERENCE"
)

package kotlinx.metadata.jvm

import kotlinx.metadata.*
import kotlinx.metadata.internal.accept
import kotlinx.metadata.jvm.internal.IgnoreInApiDump
import kotlinx.metadata.jvm.KotlinClassMetadata.Companion.COMPATIBLE_METADATA_VERSION
import kotlinx.metadata.jvm.internal.wrapIntoMetadataExceptionWhenNeeded
import kotlinx.metadata.jvm.internal.wrapWriteIntoIAE
import org.jetbrains.kotlin.metadata.jvm.JvmModuleProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping
import org.jetbrains.kotlin.metadata.jvm.deserialization.PackageParts
import org.jetbrains.kotlin.metadata.jvm.deserialization.serializeToByteArray

/**
 * Represents the parsed metadata of a Kotlin JVM module file.
 *
 * To create an instance of [KotlinModuleMetadata], load the contents of the `.kotlin_module` file into a byte array
 * and call [KotlinModuleMetadata.read].
 *
 * @property bytes the byte array representing the contents of a `.kotlin_module` file
 */
@UnstableMetadataApi
class KotlinModuleMetadata(@Suppress("MemberVisibilityCanBePrivate") val bytes: ByteArray) {
    @get:IgnoreInApiDump
    internal val data: ModuleMapping = ModuleMapping.loadModuleMapping(
        bytes, javaClass.name,
        skipMetadataVersionCheck = false,
        isJvmPackageNameSupported = true
    ) {
        // TODO: report incorrect versions of modules
    }

    /**
     * Visits metadata of this module with a new [KmModule] instance and returns that instance.
     */
    fun toKmModule(): KmModule =
        KmModule().apply(this::accept)

    /**
     * A [KmModuleVisitor] that generates the metadata of a Kotlin JVM module file.
     */
    @Deprecated("Writer API is deprecated as excessive and cumbersome. Please use KotlinModuleMetadata.write(kmModule, metadataVersion)")
    class Writer : KmModuleVisitor() {
        private val b = JvmModuleProtoBuf.Module.newBuilder()

        override fun visitPackageParts(fqName: String, fileFacades: List<String>, multiFileClassParts: Map<String, String>) {
            PackageParts(fqName).apply {
                for (fileFacade in fileFacades) {
                    addPart(fileFacade, null)
                }
                for ((multiFileClassPart, multiFileFacade) in multiFileClassParts) {
                    addPart(multiFileClassPart, multiFileFacade)
                }

                addTo(b)
            }
        }

        override fun visitAnnotation(annotation: KmAnnotation) {
            /*
            // TODO: move StringTableImpl to module 'metadata' and support module annotations here
            b.addAnnotation(ProtoBuf.Annotation.newBuilder().apply {
                id = annotation.className.name // <-- use StringTableImpl here
            })
            */
        }

        override fun visitOptionalAnnotationClass(): KmClassVisitor? {
            /*
            return object : ClassWriter(TODO() /* use StringTableImpl here */) {
                override fun visitEnd() {
                    b.addOptionalAnnotationClass(t)
                }
            }
            */
            return null
        }

        /**
         * Returns the metadata of the module file that was written with this writer.
         *
         * @param metadataVersion metadata version to be written to the metadata (see [Metadata.metadataVersion]),
         *   [KotlinClassMetadata.COMPATIBLE_METADATA_VERSION] by default
         */
        @Deprecated("Writer API is deprecated as excessive and cumbersome. Please use KotlinModuleMetadata.write(kmModule, metadataVersion)")
        fun write(metadataVersion: IntArray = COMPATIBLE_METADATA_VERSION): KotlinModuleMetadata =
            KotlinModuleMetadata(b.build().serializeToByteArray(JvmMetadataVersion(*metadataVersion), 0))
    }

    /**
     * Makes the given visitor visit metadata of this module file.
     *
     * @param v the visitor that must visit this module file
     */
    @Deprecated(VISITOR_API_MESSAGE)
    fun accept(v: KmModuleVisitor) {
        for ((fqName, parts) in data.packageFqName2Parts) {
            val (fileFacades, multiFileClassParts) = parts.parts.partition { parts.getMultifileFacadeName(it) == null }
            v.visitPackageParts(fqName, fileFacades, multiFileClassParts.associateWith { parts.getMultifileFacadeName(it)!! })
        }

        for (annotation in data.moduleData.annotations) {
            v.visitAnnotation(KmAnnotation(annotation, emptyMap()))
        }

        for (classProto in data.moduleData.optionalAnnotations) {
            v.visitOptionalAnnotationClass()?.let {
                classProto.accept(it, data.moduleData.nameResolver)
            }
        }

        v.visitEnd()
    }

    companion object {
        /**
         * Parses the given byte array with the .kotlin_module file content and returns the [KotlinModuleMetadata] instance,
         * or `null` if this byte array encodes a module with an unsupported metadata version.
         *
         * @throws IllegalArgumentException if an error happened while parsing the given byte array,
         * which means that it's either not the content of a .kotlin_module file, or it has been corrupted.
         */
        @JvmStatic
        @UnstableMetadataApi
        fun read(bytes: ByteArray): KotlinModuleMetadata? {
            return wrapIntoMetadataExceptionWhenNeeded {
                val result = KotlinModuleMetadata(bytes)
                when (result.data) {
                    ModuleMapping.EMPTY -> null
                    ModuleMapping.CORRUPTED ->
                        throw InconsistentKotlinMetadataException("Data is not the content of a .kotlin_module file, or it has been corrupted.")
                    else -> result
                }
            }
        }

        /**
         * Writes the metadata of the Kotlin module file.
         *
         * @param metadataVersion metadata version to be written to the metadata (see [Metadata.metadataVersion]),
         *   [KotlinClassMetadata.COMPATIBLE_METADATA_VERSION] by default
         *
         * @throws IllegalArgumentException if [kmModule] is not correct and cannot be written or if [metadataVersion] is not supported for writing.
         */
        @UnstableMetadataApi
        fun write(kmModule: KmModule, metadataVersion: IntArray = COMPATIBLE_METADATA_VERSION): KotlinModuleMetadata = wrapWriteIntoIAE {
            Writer().also { kmModule.accept(it) }.write(metadataVersion)
        }
    }
}

/**
 * A visitor to visit Kotlin JVM module files.
 *
 * When using this class, [visitEnd] must be called exactly once and after calls to all other visit* methods.
 */
@Deprecated(VISITOR_API_MESSAGE)
@UnstableMetadataApi
abstract class KmModuleVisitor(private val delegate: KmModuleVisitor? = null) {
    /**
     * Visits the table of all single- and multi-file facades declared in some package of this module.
     *
     * Packages are separated by '/' in the names of file facades.
     *
     * @param fqName the fully qualified name of the package, separated by '.'
     * @param fileFacades the list of single-file facades in this package
     * @param multiFileClassParts the map of multi-file classes where keys are names of multi-file class parts,
     *   and values are names of the corresponding multi-file facades
     */
    open fun visitPackageParts(fqName: String, fileFacades: List<String>, multiFileClassParts: Map<String, String>) {
        delegate?.visitPackageParts(fqName, fileFacades, multiFileClassParts)
    }

    /**
     * Visits the annotation on the module.
     *
     * @param annotation annotation on the module
     */
    open fun visitAnnotation(annotation: KmAnnotation) {
        delegate?.visitAnnotation(annotation)
    }

    /**
     * Visits an `@OptionalExpectation`-annotated annotation class declared in this module.
     * Such classes are not materialized to bytecode on JVM, but the Kotlin compiler stores their metadata in the module file on JVM,
     * and loads it during compilation of dependent modules, in order to avoid reporting "unresolved reference" errors on usages.
     *
     * Multiplatform projects are an experimental feature of Kotlin, and their behavior and/or binary format
     * may change in a subsequent release.
     */
    open fun visitOptionalAnnotationClass(): KmClassVisitor? =
        delegate?.visitOptionalAnnotationClass()

    /**
     * Visits the end of the module.
     */
    open fun visitEnd() {
        delegate?.visitEnd()
    }

    // TODO: JvmPackageName
}

/**
 * Represents a Kotlin JVM module file.
 */
@UnstableMetadataApi
class KmModule : KmModuleVisitor() {
    /**
     * Table of all single- and multi-file facades declared in some package of this module, where keys are '.'-separated package names.
     */
    val packageParts: MutableMap<String, KmPackageParts> = LinkedHashMap()

    /**
     * Annotations on the module.
     */
    val annotations: MutableList<KmAnnotation> = ArrayList(0)

    /**
     * `@OptionalExpectation`-annotated annotation classes declared in this module.
     * Such classes are not materialized to bytecode on JVM, but the Kotlin compiler stores their metadata in the module file on JVM,
     * and loads it during compilation of dependent modules, in order to avoid reporting "unresolved reference" errors on usages.
     *
     * Multiplatform projects are an experimental feature of Kotlin, and their behavior and/or binary format
     * may change in a subsequent release.
     */
    val optionalAnnotationClasses: MutableList<KmClass> = ArrayList(0)

    @Deprecated(VISITOR_API_MESSAGE)
    override fun visitPackageParts(fqName: String, fileFacades: List<String>, multiFileClassParts: Map<String, String>) {
        packageParts[fqName] = KmPackageParts(fileFacades.toMutableList(), multiFileClassParts.toMutableMap())
    }

    @Deprecated(VISITOR_API_MESSAGE)
    override fun visitAnnotation(annotation: KmAnnotation) {
        annotations.add(annotation)
    }

    @Deprecated(VISITOR_API_MESSAGE)
    override fun visitOptionalAnnotationClass(): KmClass =
        KmClass().also(optionalAnnotationClasses::add)

    /**
     * Populates the given visitor with data in this module.
     *
     * @param visitor the visitor which will visit data in this module.
     */
    @Deprecated(VISITOR_API_MESSAGE)
    fun accept(visitor: KmModuleVisitor) {
        for ((fqName, parts) in packageParts) {
            visitor.visitPackageParts(fqName, parts.fileFacades, parts.multiFileClassParts)
        }
        annotations.forEach(visitor::visitAnnotation)
        optionalAnnotationClasses.forEach { visitor.visitOptionalAnnotationClass()?.let(it::accept) }
    }
}

/**
 * Collection of all single- and multi-file facades in a package of some module.
 *
 * Packages are separated by '/' in the names of file facades.
 *
 * @property fileFacades the list of single-file facades in this package
 * @property multiFileClassParts the map of multi-file classes where keys are names of multi-file class parts,
 *   and values are names of the corresponding multi-file facades
 */
@UnstableMetadataApi
class KmPackageParts(
    val fileFacades: MutableList<String>,
    val multiFileClassParts: MutableMap<String, String>
)
