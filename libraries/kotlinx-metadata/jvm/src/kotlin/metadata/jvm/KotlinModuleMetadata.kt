/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress(
    "DEPRECATION", // COMPATIBLE_METADATA_VERSION
    "DEPRECATION_ERROR", // KmModule.annotations
    "UNUSED_PARAMETER" // For deprecated Writer.write
)

package kotlin.metadata.jvm

import kotlin.metadata.*
import kotlin.metadata.jvm.KotlinClassMetadata.Companion.COMPATIBLE_METADATA_VERSION
import kotlin.metadata.jvm.internal.JvmReadUtils.readModuleMetadataImpl
import kotlin.metadata.jvm.internal.JvmReadUtils.throwIfNotCompatible
import kotlin.metadata.jvm.internal.wrapIntoMetadataExceptionWhenNeeded
import kotlin.metadata.jvm.internal.wrapWriteIntoIAE
import org.jetbrains.kotlin.metadata.jvm.JvmModuleProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping
import org.jetbrains.kotlin.metadata.jvm.deserialization.PackageParts
import org.jetbrains.kotlin.metadata.jvm.deserialization.serializeToByteArray
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion as CompilerMetadataVersion

/**
 * Represents the parsed metadata of a Kotlin JVM module file.
 *
 * To create an instance of [KotlinModuleMetadata], load the contents of the `.kotlin_module` file into a byte array
 * and call [KotlinModuleMetadata.read]. Then it is possible to transform the result into [KmModule] with [KotlinModuleMetadata.toKmModule].
 *
 * `.kotlin_module` file is produced per Kotlin compilation, and contains auxiliary information, such as a map of all single- and multi-file facades ([KmModule.packageParts]),
 *  `@OptionalExpectation` declarations ([KmModule.optionalAnnotationClasses]), and module annotations ([KmModule.annotations).
 */
@UnstableMetadataApi
public class KotlinModuleMetadata public constructor(
    /**
     * [KmModule] representation of this metadata.
     *
     * Returns the same (mutable) [KmModule] instance every time.
     */
    public var kmModule: KmModule,

    /**
     * Version of this metadata.
     */
    public var version: JvmMetadataVersion,
) {
    /**
     * Visits metadata of this module with a new [KmModule] instance and returns that instance.
     */
    @Deprecated(
        "To avoid excessive copying, use .kmModule property instead. Note that it returns a view and not a copy.",
        ReplaceWith("kmModule"),
        DeprecationLevel.ERROR
    )
    public fun toKmModule(): KmModule = KmModule().apply { kmModule.accept(this) }

    /**
     * Encodes and writes this metadata of the Kotlin module file.
     *
     * This method encodes all available data, including [version].
     *
     * @throws IllegalArgumentException if [kmModule] is not correct and cannot be written or if [version] is not supported for writing.
     */
    public fun write(): ByteArray {
        val b = JvmModuleProtoBuf.Module.newBuilder()
        kmModule.packageParts.forEach { (fqName, packageParts) ->
            PackageParts(fqName).apply {
                for (fileFacade in packageParts.fileFacades) {
                    addPart(fileFacade, null)
                }
                for ((multiFileClassPart, multiFileFacade) in packageParts.multiFileClassParts) {
                    addPart(multiFileClassPart, multiFileFacade)
                }

                addTo(b)
            }
        }

        // visitAnnotation
        /*
        // TODO: move StringTableImpl to module 'metadata' and support module annotations here
        b.addAnnotation(ProtoBuf.Annotation.newBuilder().apply {
            id = annotation.className.name // <-- use StringTableImpl here
        })
        */

        // visitOptionalAnnotationClass
        /*
        return object : ClassWriter(TODO() /* use StringTableImpl here */) {
            override fun visitEnd() {
                b.addOptionalAnnotationClass(t)
            }
        }
        */

        // The only flag present in module metadata is STRICT_SEMANTICS, and it seems fine to ignore it and set 0 here.
        return b.build().serializeToByteArray(CompilerMetadataVersion(version.toIntArray(), false), 0)
    }


    /**
     * A [KmModuleVisitor] that generates the metadata of a Kotlin JVM module file.
     */
    @Deprecated(
        "Writer API is deprecated as excessive and cumbersome. Please use KotlinModuleMetadata.write(kmModule, metadataVersion)",
        level = DeprecationLevel.ERROR
    )
    public class Writer {
        /**
         * Returns the metadata of the module file that was written with this writer.
         *
         * @param metadataVersion metadata version to be written to the metadata (see [Metadata.metadataVersion]),
         *   [KotlinClassMetadata.COMPATIBLE_METADATA_VERSION] by default
         */
        @Deprecated(
            "Writer API is deprecated as excessive and cumbersome. Please use KotlinModuleMetadata.write(kmModule, metadataVersion)",
            level = DeprecationLevel.ERROR
        )
        public fun write(metadataVersion: IntArray = COMPATIBLE_METADATA_VERSION): ByteArray {
            error("This method is no longer implemented. Migrate to KotlinModuleMetadata.write.")
        }
    }

    /**
     * Makes the given visitor visit the metadata of this module file.
     *
     * @param v the visitor that must visit this module file
     */
    @Deprecated(VISITOR_API_MESSAGE, level = DeprecationLevel.ERROR)
    public fun accept(v: KmModuleVisitor): Unit = kmModule.accept(v)

    /**
     * Collection of methods for reading and writing [KotlinModuleMetadata].
     */
    public companion object {
        /**
         * Parses the given byte array with the .kotlin_module file content and returns the [KotlinModuleMetadata] instance,
         * or `null` if this byte array encodes a module with an unsupported metadata version.
         *
         * @throws IllegalArgumentException if an error happened while parsing the given byte array,
         * which means that it is either not the content of a `.kotlin_module` file, or it has been corrupted.
         */
        @JvmStatic
        @UnstableMetadataApi
        public fun read(bytes: ByteArray): KotlinModuleMetadata {
            return wrapIntoMetadataExceptionWhenNeeded {
                val result = ModuleMapping.loadModuleMapping(
                    bytes, "KotlinModuleMetadata", skipMetadataVersionCheck = false,
                    isJvmPackageNameSupported = true
                ) { throwIfNotCompatible(it, lenient = false /* TODO */) }
                when (result) {
                    ModuleMapping.EMPTY, ModuleMapping.CORRUPTED ->
                        throw IllegalArgumentException("Data is not the content of a .kotlin_module file, or it has been corrupted.")
                }
                val module = readModuleMetadataImpl(result)
                KotlinModuleMetadata(module, JvmMetadataVersion(result.version.toArray()))
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
        @Deprecated("Use a KotlinModuleMetadata instance and its write() member function", level = DeprecationLevel.ERROR)
        @JvmStatic
        @JvmOverloads
        public fun write(kmModule: KmModule, metadataVersion: JvmMetadataVersion = JvmMetadataVersion.LATEST_STABLE_SUPPORTED): ByteArray = wrapWriteIntoIAE {
            return KotlinModuleMetadata(kmModule, metadataVersion).write()
        }
    }
}

/**
 * A visitor to visit Kotlin JVM module files.
 *
 * When using this class, [visitEnd] must be called exactly once and after calls to all other visit* methods.
 */
@Deprecated(VISITOR_API_MESSAGE, level = DeprecationLevel.ERROR)
@UnstableMetadataApi
public abstract class KmModuleVisitor(private val delegate: KmModuleVisitor? = null) {
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
    public open fun visitPackageParts(fqName: String, fileFacades: List<String>, multiFileClassParts: Map<String, String>) {
        delegate?.visitPackageParts(fqName, fileFacades, multiFileClassParts)
    }

    /**
     * Visits the annotation on the module.
     *
     * @param annotation annotation on the module
     */
    public open fun visitAnnotation(annotation: KmAnnotation) {
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
    public open fun visitOptionalAnnotationClass(): KmClassVisitor? =
        delegate?.visitOptionalAnnotationClass()

    /**
     * Visits the end of the module.
     */
    public open fun visitEnd() {
        delegate?.visitEnd()
    }

    // TODO: JvmPackageName
}

/**
 * Represents a Kotlin JVM module file (`.kotlin_module` extension).
 */
@UnstableMetadataApi
public class KmModule : KmModuleVisitor() {
    /**
     * Table of all single- and multi-file facades declared in some package of this module, where keys are '.'-separated package names.
     */
    public val packageParts: MutableMap<String, KmPackageParts> = LinkedHashMap()

    /**
     * Annotations on the module.
     *
     * Currently, Kotlin does not provide functionality to specify annotations on modules.
     */
    @Deprecated("This list is always empty and will be removed", level = DeprecationLevel.ERROR)
    public val annotations: MutableList<KmAnnotation> = ArrayList(0)

    /**
     * `@OptionalExpectation`-annotated annotation classes declared in this module.
     * Such classes are not materialized to bytecode on JVM, but the Kotlin compiler stores their metadata in the module file on JVM,
     * and loads it during compilation of dependent modules, in order to avoid reporting "unresolved reference" errors on usages.
     *
     * Multiplatform projects are an experimental feature of Kotlin, and their behavior and/or binary format
     * may change in a subsequent release.
     */
    public val optionalAnnotationClasses: MutableList<KmClass> = ArrayList(0)

    @Deprecated(VISITOR_API_MESSAGE, level = DeprecationLevel.ERROR)
    override fun visitPackageParts(fqName: String, fileFacades: List<String>, multiFileClassParts: Map<String, String>) {
        packageParts[fqName] = KmPackageParts(fileFacades.toMutableList(), multiFileClassParts.toMutableMap())
    }

    @Deprecated(VISITOR_API_MESSAGE, level = DeprecationLevel.ERROR)
    override fun visitAnnotation(annotation: KmAnnotation) {
        annotations.add(annotation)
    }

    @Deprecated(VISITOR_API_MESSAGE, level = DeprecationLevel.ERROR)
    override fun visitOptionalAnnotationClass(): KmClass =
        KmClass().also(optionalAnnotationClasses::add)

    /**
     * Populates the given visitor with data in this module.
     *
     * @param visitor the visitor which will visit data in this module.
     */
    @Deprecated(VISITOR_API_MESSAGE, level = DeprecationLevel.ERROR)
    public fun accept(visitor: KmModuleVisitor) {
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
public class KmPackageParts(
    public val fileFacades: MutableList<String>,
    public val multiFileClassParts: MutableMap<String, String>
)
