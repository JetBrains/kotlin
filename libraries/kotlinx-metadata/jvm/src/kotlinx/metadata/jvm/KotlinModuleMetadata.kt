/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.jvm

import kotlinx.metadata.InconsistentKotlinMetadataException
import kotlinx.metadata.KmAnnotation
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
class KotlinModuleMetadata(@Suppress("CanBeParameter", "MemberVisibilityCanBePrivate") val bytes: ByteArray) {
    internal val data: ModuleMapping = ModuleMapping.loadModuleMapping(
        bytes, javaClass.name, skipMetadataVersionCheck = false, isJvmPackageNameSupported = true
    ) {
        // TODO: report incorrect versions of modules
    }

    /**
     * A [KmModuleVisitor] that generates the metadata of a Kotlin JVM module file.
     */
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

        /**
         * Returns the metadata of the module file that was written with this writer.
         *
         * @param metadataVersion metadata version to be written to the metadata (see [KotlinClassHeader.metadataVersion]),
         *   [KotlinClassHeader.COMPATIBLE_METADATA_VERSION] by default
         */
        fun write(metadataVersion: IntArray = KotlinClassHeader.COMPATIBLE_METADATA_VERSION): KotlinModuleMetadata =
            KotlinModuleMetadata(b.build().serializeToByteArray(JvmMetadataVersion(*metadataVersion), 0))
    }

    /**
     * Makes the given visitor visit metadata of this module file.
     *
     * @param v the visitor that must visit this module file
     */
    fun accept(v: KmModuleVisitor) {
        for ((fqName, parts) in data.packageFqName2Parts) {
            val (fileFacades, multiFileClassParts) = parts.parts.partition { parts.getMultifileFacadeName(it) == null }
            v.visitPackageParts(fqName, fileFacades, multiFileClassParts.associate { it to parts.getMultifileFacadeName(it)!! })
        }

        for (annotation in data.moduleData.annotations) {
            v.visitAnnotation(KmAnnotation(annotation, emptyMap()))
        }

        v.visitEnd()
    }

    companion object {
        /**
         * Parses the given byte array with the .kotlin_module file content and returns the [KotlinModuleMetadata] instance,
         * or `null` if this byte array encodes a module with an unsupported metadata version.
         *
         * Throws [InconsistentKotlinMetadataException] if an error happened while parsing the given byte array,
         * which means that it's either not the content of a .kotlin_module file, or it has been corrupted.
         */
        @JvmStatic
        fun read(bytes: ByteArray): KotlinModuleMetadata? {
            try {
                val result = KotlinModuleMetadata(bytes)
                if (result.data == ModuleMapping.EMPTY) return null

                if (result.data == ModuleMapping.CORRUPTED) {
                    throw InconsistentKotlinMetadataException("Data doesn't look like the content of a .kotlin_module file")
                }

                return result
            } catch (e: InconsistentKotlinMetadataException) {
                throw e
            } catch (e: Throwable) {
                throw InconsistentKotlinMetadataException("Exception occurred when reading Kotlin metadata", e)
            }
        }
    }
}

/**
 * A visitor to visit Kotlin JVM module files.
 *
 * When using this class, [visitEnd] must be called exactly once and after calls to all other visit* methods.
 */
abstract class KmModuleVisitor(private val delegate: KmModuleVisitor? = null) {
    /**
     * Visits the table of single- and multi-file facades declared in some package of this module. Note that the same package may be split
     * in several "chunks", in which case this method is called several times for one package, once for each chunk.
     *
     * Packages are separated by '/' in the names of file facades.
     *
     * @param fqName the fully qualified name of the package
     * @param fileFacades the list of single-file facades in this chunk of the package
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
     * Visits the end of the module.
     */
    open fun visitEnd() {
        delegate?.visitEnd()
    }

    // TODO: JvmPackageName
}
