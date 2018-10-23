/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.jvm

import kotlinx.metadata.InconsistentKotlinMetadataException
import kotlinx.metadata.KmClassVisitor
import kotlinx.metadata.KmLambdaVisitor
import kotlinx.metadata.KmPackageVisitor
import kotlinx.metadata.impl.ClassWriter
import kotlinx.metadata.impl.LambdaWriter
import kotlinx.metadata.impl.PackageWriter
import kotlinx.metadata.impl.accept
import kotlinx.metadata.jvm.impl.writeProtoBufData
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.metadata.jvm.serialization.JvmStringTable
import kotlin.LazyThreadSafetyMode.PUBLICATION

/**
 * Represents the parsed metadata of a Kotlin JVM class file.
 *
 * To create an instance of [KotlinClassMetadata], first obtain a [KotlinClassHeader] instance by loading the contents
 * of the [Metadata] annotation on a class file, and then call [KotlinClassMetadata.read].
 */
sealed class KotlinClassMetadata(val header: KotlinClassHeader) {
    /**
     * Represents metadata of a class file containing a declaration of a Kotlin class.
     */
    class Class internal constructor(header: KotlinClassHeader) : KotlinClassMetadata(header) {
        private val classData by lazy(PUBLICATION) {
            val data1 = (header.data1.takeIf(Array<*>::isNotEmpty)
                    ?: throw InconsistentKotlinMetadataException("data1 must not be empty"))
            JvmProtoBufUtil.readClassDataFrom(data1, header.data2)
        }

        /**
         * Makes the given visitor visit metadata of this class.
         *
         * @param v the visitor that must visit this class
         */
        fun accept(v: KmClassVisitor) {
            val (strings, proto) = classData
            proto.accept(v, strings)
        }

        /**
         * A [KmClassVisitor] that generates the metadata of a Kotlin class.
         */
        class Writer : ClassWriter(JvmStringTable()) {
            /**
             * Returns the metadata of the class that was written with this writer.
             *
             * @param metadataVersion metadata version to be written to the metadata (see [KotlinClassHeader.metadataVersion]),
             *   [KotlinClassHeader.COMPATIBLE_METADATA_VERSION] by default
             * @param bytecodeVersion bytecode version to be written to the metadata (see [KotlinClassHeader.bytecodeVersion]),
             *   [KotlinClassHeader.COMPATIBLE_BYTECODE_VERSION] by default
             * @param extraInt the value of the class-level flags to be written to the metadata (see [KotlinClassHeader.extraInt]),
             *   0 by default
             */
            @JvmOverloads
            fun write(
                metadataVersion: IntArray = KotlinClassHeader.COMPATIBLE_METADATA_VERSION,
                bytecodeVersion: IntArray = KotlinClassHeader.COMPATIBLE_BYTECODE_VERSION,
                extraInt: Int = 0
            ): KotlinClassMetadata.Class {
                val (d1, d2) = writeProtoBufData(t.build(), c)
                val metadata = KotlinClassHeader(
                    KotlinClassHeader.CLASS_KIND, metadataVersion, bytecodeVersion, d1, d2, null, null, extraInt
                )
                return KotlinClassMetadata.Class(metadata)
            }
        }
    }

    /**
     * Represents metadata of a class file containing a compiled Kotlin file facade.
     */
    class FileFacade internal constructor(header: KotlinClassHeader) : KotlinClassMetadata(header) {
        private val packageData by lazy(PUBLICATION) {
            val data1 = (header.data1.takeIf(Array<*>::isNotEmpty)
                    ?: throw InconsistentKotlinMetadataException("data1 must not be empty"))
            JvmProtoBufUtil.readPackageDataFrom(data1, header.data2)
        }

        /**
         * Makes the given visitor visit metadata of this file facade.
         *
         * @param v the visitor that must visit this file facade
         */
        fun accept(v: KmPackageVisitor) {
            val (strings, proto) = packageData
            proto.accept(v, strings)
        }

        /**
         * A [KmPackageVisitor] that generates the metadata of a Kotlin file facade.
         */
        class Writer : PackageWriter(JvmStringTable()) {
            /**
             * Returns the metadata of the file facade that was written with this writer.
             *
             * @param metadataVersion metadata version to be written to the metadata (see [KotlinClassHeader.metadataVersion]),
             *   [KotlinClassHeader.COMPATIBLE_METADATA_VERSION] by default
             * @param bytecodeVersion bytecode version to be written to the metadata (see [KotlinClassHeader.bytecodeVersion]),
             *   [KotlinClassHeader.COMPATIBLE_BYTECODE_VERSION] by default
             * @param extraInt the value of the class-level flags to be written to the metadata (see [KotlinClassHeader.extraInt]),
             *   0 by default
             */
            @JvmOverloads
            fun write(
                metadataVersion: IntArray = KotlinClassHeader.COMPATIBLE_METADATA_VERSION,
                bytecodeVersion: IntArray = KotlinClassHeader.COMPATIBLE_BYTECODE_VERSION,
                extraInt: Int = 0
            ): KotlinClassMetadata.FileFacade {
                val (d1, d2) = writeProtoBufData(t.build(), c)
                val metadata = KotlinClassHeader(
                    KotlinClassHeader.FILE_FACADE_KIND, metadataVersion, bytecodeVersion, d1, d2, null, null, extraInt
                )
                return KotlinClassMetadata.FileFacade(metadata)
            }
        }
    }

    /**
     * Represents metadata of a class file containing a synthetic class, e.g. a class for lambda, `$DefaultImpls` class for interface
     * method implementations, `$WhenMappings` class for optimized `when` over enums, etc.
     */
    class SyntheticClass internal constructor(header: KotlinClassHeader) : KotlinClassMetadata(header) {
        private val functionData by lazy(PUBLICATION) {
            header.data1.takeIf(Array<*>::isNotEmpty)?.let { data1 ->
                JvmProtoBufUtil.readFunctionDataFrom(data1, header.data2)
            }
        }

        /**
         * Returns `true` if this synthetic class is a class file compiled for a Kotlin lambda.
         */
        val isLambda: Boolean
            get() = header.data1.isNotEmpty()

        /**
         * Makes the given visitor visit metadata of this file facade, if this synthetic class represents a Kotlin lambda
         * (`isLambda` == true).
         *
         * Throws [IllegalStateException] if this synthetic class does not represent a Kotlin lambda.
         *
         * @param v the visitor that must visit this lambda
         */
        fun accept(v: KmLambdaVisitor) {
            if (!isLambda) throw IllegalStateException(
                "accept(KmLambdaVisitor) is only possible for synthetic classes which are lambdas (isLambda = true)"
            )

            val (strings, proto) = functionData!!
            proto.accept(v, strings)
        }

        /**
         * A [KmLambdaVisitor] that generates the metadata of a synthetic class. To generate metadata of a Kotlin lambda,
         * call [Writer.visitFunction] and [Writer.visitEnd] on a newly created instance of this writer. If these methods are not called,
         * the resulting metadata will represent a _non-lambda_ synthetic class.
         */
        class Writer : LambdaWriter(JvmStringTable()) {
            /**
             * Returns the metadata of the synthetic class that was written with this writer.
             *
             * @param metadataVersion metadata version to be written to the metadata (see [KotlinClassHeader.metadataVersion]),
             *   [KotlinClassHeader.COMPATIBLE_METADATA_VERSION] by default
             * @param bytecodeVersion bytecode version to be written to the metadata (see [KotlinClassHeader.bytecodeVersion]),
             *   [KotlinClassHeader.COMPATIBLE_BYTECODE_VERSION] by default
             * @param extraInt the value of the class-level flags to be written to the metadata (see [KotlinClassHeader.extraInt]),
             *   0 by default
             */
            @JvmOverloads
            fun write(
                metadataVersion: IntArray = KotlinClassHeader.COMPATIBLE_METADATA_VERSION,
                bytecodeVersion: IntArray = KotlinClassHeader.COMPATIBLE_BYTECODE_VERSION,
                extraInt: Int = 0
            ): KotlinClassMetadata.SyntheticClass {
                val proto = t?.build()
                val (d1, d2) =
                        if (proto != null) writeProtoBufData(proto, c)
                        else Pair(emptyArray(), emptyArray())
                val metadata = KotlinClassHeader(
                    KotlinClassHeader.SYNTHETIC_CLASS_KIND, metadataVersion, bytecodeVersion, d1, d2, null, null, extraInt
                )
                return KotlinClassMetadata.SyntheticClass(metadata)
            }
        }
    }

    /**
     * Represents metadata of a class file containing a compiled multi-file class facade.
     *
     * @see JvmMultifileClass
     */
    class MultiFileClassFacade internal constructor(header: KotlinClassHeader) : KotlinClassMetadata(header) {
        /**
         * JVM internal names of the part classes which this multi-file class combines.
         */
        val partClassNames: List<String> = header.data1.asList()

        /**
         * A writer that generates the metadata of a multi-file class facade.
         */
        class Writer {
            /**
             * Returns the metadata of the multi-file class facade that was written with this writer.
             *
             * @param partClassNames JVM internal names of the part classes which this multi-file class combines
             * @param metadataVersion metadata version to be written to the metadata (see [KotlinClassHeader.metadataVersion]),
             *   [KotlinClassHeader.COMPATIBLE_METADATA_VERSION] by default
             * @param bytecodeVersion bytecode version to be written to the metadata (see [KotlinClassHeader.bytecodeVersion]),
             *   [KotlinClassHeader.COMPATIBLE_BYTECODE_VERSION] by default
             * @param extraInt the value of the class-level flags to be written to the metadata (see [KotlinClassHeader.extraInt]),
             *   0 by default
             */
            @JvmOverloads
            fun write(
                partClassNames: List<String>,
                metadataVersion: IntArray = KotlinClassHeader.COMPATIBLE_METADATA_VERSION,
                bytecodeVersion: IntArray = KotlinClassHeader.COMPATIBLE_BYTECODE_VERSION,
                extraInt: Int = 0
            ): KotlinClassMetadata.MultiFileClassFacade {
                val metadata = KotlinClassHeader(
                    KotlinClassHeader.MULTI_FILE_CLASS_FACADE_KIND, metadataVersion, bytecodeVersion, partClassNames.toTypedArray(),
                    null, null, null, extraInt
                )
                return KotlinClassMetadata.MultiFileClassFacade(metadata)
            }
        }
    }

    /**
     * Represents metadata of a class file containing a compiled multi-file class part, i.e. an internal class with method bodies
     * and their metadata, accessed only from the corresponding facade.
     *
     * @see JvmMultifileClass
     */
    class MultiFileClassPart internal constructor(header: KotlinClassHeader) : KotlinClassMetadata(header) {
        private val packageData by lazy(PUBLICATION) {
            val data1 = (header.data1.takeIf(Array<*>::isNotEmpty)
                    ?: throw InconsistentKotlinMetadataException("data1 must not be empty"))
            JvmProtoBufUtil.readPackageDataFrom(data1, header.data2)
        }

        /**
         * JVM internal name of the corresponding multi-file class facade.
         */
        val facadeClassName: String
            get() = header.extraString

        /**
         * Makes the given visitor visit metadata of this multi-file class part.
         *
         * @param v the visitor that must visit this multi-file class part
         */
        fun accept(v: KmPackageVisitor) {
            val (strings, proto) = packageData
            proto.accept(v, strings)
        }

        /**
         * A [KmPackageVisitor] that generates the metadata of a multi-file class part.
         */
        class Writer : PackageWriter(JvmStringTable()) {
            /**
             * Returns the metadata of the multi-file class part that was written with this writer.
             *
             * @param facadeClassName JVM internal name of the corresponding multi-file class facade
             * @param metadataVersion metadata version to be written to the metadata (see [KotlinClassHeader.metadataVersion]),
             *   [KotlinClassHeader.COMPATIBLE_METADATA_VERSION] by default
             * @param bytecodeVersion bytecode version to be written to the metadata (see [KotlinClassHeader.bytecodeVersion]),
             *   [KotlinClassHeader.COMPATIBLE_BYTECODE_VERSION] by default
             * @param extraInt the value of the class-level flags to be written to the metadata (see [KotlinClassHeader.extraInt]),
             *   0 by default
             */
            @JvmOverloads
            fun write(
                facadeClassName: String,
                metadataVersion: IntArray = KotlinClassHeader.COMPATIBLE_METADATA_VERSION,
                bytecodeVersion: IntArray = KotlinClassHeader.COMPATIBLE_BYTECODE_VERSION,
                extraInt: Int = 0
            ): KotlinClassMetadata.MultiFileClassPart {
                val (d1, d2) = writeProtoBufData(t.build(), c)
                val metadata = KotlinClassHeader(
                    KotlinClassHeader.MULTI_FILE_CLASS_PART_KIND, metadataVersion, bytecodeVersion, d1, d2, facadeClassName, null, extraInt
                )
                return KotlinClassMetadata.MultiFileClassPart(metadata)
            }
        }
    }

    /**
     * Represents metadata of an unknown class file. This class is used if an old version of this library is used against a new kind
     * of class files generated by the Kotlin compiler, unsupported by this library.
     */
    class Unknown internal constructor(header: KotlinClassHeader) : KotlinClassMetadata(header)

    companion object {
        /**
         * Reads and parses the given header of a Kotlin JVM class file and returns the correct type of [KotlinClassMetadata] encoded by
         * this header, or `null` if this header encodes an unsupported kind of Kotlin classes or has an unsupported metadata version.
         *
         * Throws [InconsistentKotlinMetadataException] if the metadata has inconsistencies which signal that it may have been
         * modified by a separate tool.
         *
         * @param header the header of a Kotlin JVM class file to be parsed
         */
        @JvmStatic
        fun read(header: KotlinClassHeader): KotlinClassMetadata? {
            // We only support metadata of version 1.1.* (this is Kotlin from 1.0 until today)
            if (!JvmMetadataVersion(
                    header.metadataVersion,
                    (header.extraInt and (1 shl 3)/* see JvmAnnotationNames.METADATA_STRICT_VERSION_SEMANTICS_FLAG */) != 0
                ).isCompatible()
            ) return null

            return try {
                when (header.kind) {
                    KotlinClassHeader.CLASS_KIND -> KotlinClassMetadata.Class(header)
                    KotlinClassHeader.FILE_FACADE_KIND -> KotlinClassMetadata.FileFacade(header)
                    KotlinClassHeader.SYNTHETIC_CLASS_KIND -> KotlinClassMetadata.SyntheticClass(header)
                    KotlinClassHeader.MULTI_FILE_CLASS_FACADE_KIND -> KotlinClassMetadata.MultiFileClassFacade(header)
                    KotlinClassHeader.MULTI_FILE_CLASS_PART_KIND -> KotlinClassMetadata.MultiFileClassPart(header)
                    else -> KotlinClassMetadata.Unknown(header)
                }
            } catch (e: InconsistentKotlinMetadataException) {
                throw e
            } catch (e: Throwable) {
                throw InconsistentKotlinMetadataException("Exception occurred when reading Kotlin metadata", e)
            }
        }
    }
}
