/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.metadata.jvm

import kotlin.metadata.*
import kotlin.metadata.jvm.internal.JvmReadUtils.readModuleMetadataImpl
import kotlin.metadata.jvm.internal.JvmReadUtils.throwIfNotCompatible
import kotlin.metadata.jvm.internal.wrapIntoMetadataExceptionWhenNeeded
import org.jetbrains.kotlin.metadata.jvm.JvmModuleProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping
import org.jetbrains.kotlin.metadata.jvm.deserialization.PackageParts
import org.jetbrains.kotlin.metadata.jvm.deserialization.serializeToByteArray
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion as CompilerMetadataVersion

/**
 * Represents the parsed metadata of a Kotlin JVM module file.
 *
 * To create an instance of [KotlinModuleMetadata], load the contents of the `.kotlin_module` file into a byte array
 * and call [KotlinModuleMetadata.read]. Then it is possible to get the result in the form of [KmModule] with [KotlinModuleMetadata.kmModule].
 *
 * `.kotlin_module` file is produced per Kotlin compilation, and contains auxiliary information, such as a map of all single- and multi-file facades ([KmModule.packageParts]),
 *  and `@OptionalExpectation` declarations ([KmModule.optionalAnnotationClasses]).
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
    }
}

/**
 * Represents a Kotlin JVM module file (`.kotlin_module` extension).
 */
@UnstableMetadataApi
public class KmModule {
    /**
     * Table of all single- and multi-file facades declared in some package of this module, where keys are '.'-separated package names.
     */
    public val packageParts: MutableMap<String, KmPackageParts> = LinkedHashMap()

    /**
     * `@OptionalExpectation`-annotated annotation classes declared in this module.
     * Such classes are not materialized to bytecode on JVM, but the Kotlin compiler stores their metadata in the module file on JVM,
     * and loads it during compilation of dependent modules, to avoid reporting "unresolved reference" errors on usages.
     */
    public val optionalAnnotationClasses: MutableList<KmClass> = ArrayList(0)
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
