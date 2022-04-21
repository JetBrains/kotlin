/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.jvm

import kotlinx.metadata.*
import kotlinx.metadata.impl.ClassWriter
import kotlinx.metadata.impl.LambdaWriter
import kotlinx.metadata.impl.PackageWriter
import kotlinx.metadata.impl.accept
import kotlinx.metadata.jvm.impl.writeProtoBufData
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.metadata.jvm.serialization.JvmStringTable
import java.util.*
import kotlin.LazyThreadSafetyMode.PUBLICATION

/**
 * Represents the parsed metadata of a Kotlin JVM class file.
 *
 * To create an instance of [KotlinClassMetadata], first obtain a [KotlinClassHeader] instance by loading the contents
 * of the [Metadata] annotation on a class file, and then call [KotlinClassMetadata.read].
 */
sealed class KotlinClassMetadata(val header: Metadata) {
    /**
     * Represents metadata of a class file containing a declaration of a Kotlin class.
     */
    class Class internal constructor(header: Metadata) : KotlinClassMetadata(header) {
        private val classData by lazy(PUBLICATION) {
            val data1 = (header.data1.takeIf(Array<*>::isNotEmpty)
                ?: throw InconsistentKotlinMetadataException("data1 must not be empty"))
            JvmProtoBufUtil.readClassDataFrom(data1, header.data2)
        }

        /**
         * Visits metadata of this class with a new [KmClass] instance and returns that instance.
         */
        @OptIn(DeprecatedVisitor::class)
        fun toKmClass(): KmClass =
            KmClass().apply(this::accept)

        /**
         * Makes the given visitor visit metadata of this class.
         *
         * @param v the visitor that must visit this class
         */
        @DeprecatedVisitor
        fun accept(v: KmClassVisitor) {
            val (strings, proto) = classData
            proto.accept(v, strings)
        }

        /**
         * A [KmClassVisitor] that generates the metadata of a Kotlin class.
         */
        @OptIn(DeprecatedVisitor::class)
        @Deprecated(
            "Writer API is deprecated. Please use KotlinClassMetadata.writeClass(kmClass, metadataVersion, extraInt)",
            level = DeprecationLevel.ERROR
        )
        class Writer : ClassWriter(JvmStringTable()) {
            /**
             * Returns the metadata of the class that was written with this writer.
             *
             * @param metadataVersion metadata version to be written to the metadata (see [KotlinClassHeader.metadataVersion]),
             *   [KotlinClassMetadata.COMPATIBLE_METADATA_VERSION] by default. Cannot be less (lexicographically) than `[1, 4]`
             * @param extraInt the value of the class-level flags to be written to the metadata (see [KotlinClassHeader.extraInt]),
             *   0 by default
             */
            @JvmOverloads
            @Deprecated(
                "Writer API is deprecated. Please use KotlinClassMetadata.writeClass(kmClass, metadataVersion, extraInt)",
                level = DeprecationLevel.ERROR
            )
            fun write(
                metadataVersion: IntArray = COMPATIBLE_METADATA_VERSION,
                extraInt: Int = 0
            ): Class {
                checkMetadataVersion(metadataVersion)
                val (d1, d2) = writeProtoBufData(t.build(), c)
                val metadata = Metadata(CLASS_KIND, metadataVersion, d1, d2, extraInt = extraInt)
                return Class(metadata)
            }
        }
    }

    /**
     * Represents metadata of a class file containing a compiled Kotlin file facade.
     */
    class FileFacade internal constructor(header: Metadata) : KotlinClassMetadata(header) {
        private val packageData by lazy(PUBLICATION) {
            val data1 = (header.data1.takeIf(Array<*>::isNotEmpty)
                ?: throw InconsistentKotlinMetadataException("data1 must not be empty"))
            JvmProtoBufUtil.readPackageDataFrom(data1, header.data2)
        }

        /**
         * Visits metadata of this file facade with a new [KmPackage] instance and returns that instance.
         */
        @OptIn(DeprecatedVisitor::class)
        fun toKmPackage(): KmPackage =
            KmPackage().apply(this::accept)

        /**
         * Makes the given visitor visit metadata of this file facade.
         *
         * @param v the visitor that must visit this file facade
         */
        @DeprecatedVisitor
        fun accept(v: KmPackageVisitor) {
            val (strings, proto) = packageData
            proto.accept(v, strings)
        }

        /**
         * A [KmPackageVisitor] that generates the metadata of a Kotlin file facade.
         */
        @OptIn(DeprecatedVisitor::class)
        @Deprecated(
            "Writer API is deprecated. Please use KotlinClassMetadata.writeFileFacade(kmPackage, metadataVersion, extraInt)",
            level = DeprecationLevel.ERROR
        )
        class Writer : PackageWriter(JvmStringTable()) {
            /**
             * Returns the metadata of the file facade that was written with this writer.
             *
             * @param metadataVersion metadata version to be written to the metadata (see [KotlinClassHeader.metadataVersion]),
             *   [KotlinClassMetadata.COMPATIBLE_METADATA_VERSION] by default. Cannot be less (lexicographically) than `[1, 4]`
             * @param extraInt the value of the class-level flags to be written to the metadata (see [KotlinClassHeader.extraInt]),
             *   0 by default
             */
            @JvmOverloads
            @Deprecated(
                "Writer API is deprecated. Please use KotlinClassMetadata.writeFileFacade(kmPackage, metadataVersion, extraInt)",
                level = DeprecationLevel.ERROR
            )
            fun write(
                metadataVersion: IntArray = COMPATIBLE_METADATA_VERSION,
                extraInt: Int = 0
            ): FileFacade {
                checkMetadataVersion(metadataVersion)
                val (d1, d2) = writeProtoBufData(t.build(), c)
                val metadata = Metadata(FILE_FACADE_KIND, metadataVersion, d1, d2, extraInt = extraInt)
                return FileFacade(metadata)
            }
        }
    }

    /**
     * Represents metadata of a class file containing a synthetic class, e.g. a class for lambda, `$DefaultImpls` class for interface
     * method implementations, `$WhenMappings` class for optimized `when` over enums, etc.
     */
    class SyntheticClass internal constructor(header: Metadata) : KotlinClassMetadata(header) {
        private val functionData by lazy(PUBLICATION) {
            header.data1.takeIf(Array<*>::isNotEmpty)?.let { data1 ->
                JvmProtoBufUtil.readFunctionDataFrom(data1, header.data2)
            }
        }

        /**
         * Visits metadata of this synthetic class with a new [KmLambda] instance and returns that instance.
         *
         * Returns `null` if this synthetic class does not represent a lambda.
         */
        @OptIn(DeprecatedVisitor::class)
        fun toKmLambda(): KmLambda? =
            if (isLambda) KmLambda().apply(this::accept) else null

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
        @DeprecatedVisitor
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
        @OptIn(DeprecatedVisitor::class)
        @Deprecated(
            "Writer API is deprecated. Please use KotlinClassMetadata.writeLambda(kmLambda, metadataVersion, extraInt) " +
                    "or KotlinClassMetadata.writeSyntheticClass(metadataVersion, extraInt) for a non-lambda synthetic class",
            level = DeprecationLevel.ERROR
        )
        class Writer : LambdaWriter(JvmStringTable()) {
            /**
             * Returns the metadata of the synthetic class that was written with this writer.
             *
             * @param metadataVersion metadata version to be written to the metadata (see [KotlinClassHeader.metadataVersion]),
             *   [KotlinClassMetadata.COMPATIBLE_METADATA_VERSION] by default. Cannot be less (lexicographically) than `[1, 4]`
             * @param extraInt the value of the class-level flags to be written to the metadata (see [KotlinClassHeader.extraInt]),
             *   0 by default
             */
            @Deprecated(
                "Writer API is deprecated. Please use KotlinClassMetadata.writeLambda(kmLambda, metadataVersion, extraInt) " +
                        "or KotlinClassMetadata.writeSyntheticClass(metadataVersion, extraInt) for a non-lambda synthetic class",
                level = DeprecationLevel.ERROR
            )
            @JvmOverloads
            fun write(
                metadataVersion: IntArray = COMPATIBLE_METADATA_VERSION,
                extraInt: Int = 0
            ): SyntheticClass {
                checkMetadataVersion(metadataVersion)
                val proto = t?.build()
                val (d1, d2) =
                    if (proto != null) writeProtoBufData(proto, c)
                    else Pair(emptyArray(), emptyArray())
                val metadata = Metadata(SYNTHETIC_CLASS_KIND, metadataVersion, d1, d2, extraInt = extraInt)
                return SyntheticClass(metadata)
            }
        }
    }

    /**
     * Represents metadata of a class file containing a compiled multi-file class facade.
     *
     * @see JvmMultifileClass
     */
    class MultiFileClassFacade internal constructor(header: Metadata) : KotlinClassMetadata(header) {
        /**
         * JVM internal names of the part classes which this multi-file class combines.
         */
        val partClassNames: List<String> = header.data1.asList()

        /**
         * A writer that generates the metadata of a multi-file class facade.
         */
        @Deprecated(
            "Writer API is deprecated. Please use KotlinClassMetadata.writeMultiFileClassFacade(partClassNames, metadataVersion, extraInt)",
            level = DeprecationLevel.ERROR
        )
        class Writer {
            /**
             * Returns the metadata of the multi-file class facade that was written with this writer.
             *
             * @param partClassNames JVM internal names of the part classes which this multi-file class combines
             * @param metadataVersion metadata version to be written to the metadata (see [KotlinClassHeader.metadataVersion]),
             *   [KotlinClassMetadata.COMPATIBLE_METADATA_VERSION] by default. Cannot be less (lexicographically) than `[1, 4]`
             * @param extraInt the value of the class-level flags to be written to the metadata (see [KotlinClassHeader.extraInt]),
             *   0 by default
             */
            @Deprecated(
                "Writer API is deprecated. Please use KotlinClassMetadata.writeMultiFileClassFacade(partClassNames, metadataVersion, extraInt)",
                ReplaceWith("KotlinClassMetadata.writeMultiFileClassFacade(partClassNames, metadataVersion, extraInt)"),
                level = DeprecationLevel.ERROR
            )
            @JvmOverloads
            fun write(
                partClassNames: List<String>,
                metadataVersion: IntArray = COMPATIBLE_METADATA_VERSION,
                extraInt: Int = 0
            ): MultiFileClassFacade {
                checkMetadataVersion(metadataVersion)
                val metadata = Metadata(
                    MULTI_FILE_CLASS_FACADE_KIND, metadataVersion,
                    partClassNames.toTypedArray(), extraInt = extraInt
                )
                return MultiFileClassFacade(metadata)
            }
        }
    }

    /**
     * Represents metadata of a class file containing a compiled multi-file class part, i.e. an internal class with method bodies
     * and their metadata, accessed only from the corresponding facade.
     *
     * @see JvmMultifileClass
     */
    class MultiFileClassPart internal constructor(header: Metadata) : KotlinClassMetadata(header) {
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
         * Visits metadata of this multi-file class part with a new [KmPackage] instance and returns that instance.
         */
        @OptIn(DeprecatedVisitor::class)
        fun toKmPackage(): KmPackage =
            KmPackage().apply(this::accept)

        /**
         * Makes the given visitor visit metadata of this multi-file class part.
         *
         * @param v the visitor that must visit this multi-file class part
         */
        @DeprecatedVisitor
        fun accept(v: KmPackageVisitor) {
            val (strings, proto) = packageData
            proto.accept(v, strings)
        }

        /**
         * A [KmPackageVisitor] that generates the metadata of a multi-file class part.
         */
        @OptIn(DeprecatedVisitor::class)
        @Deprecated(
            "Writer API is deprecated. Please use KotlinClassMetadata.writeMultiFileClassPart(kmPackage, facadeClassName, metadataVersion, extraInt)",
            level = DeprecationLevel.ERROR
        )
        class Writer : PackageWriter(JvmStringTable()) {
            /**
             * Returns the metadata of the multi-file class part that was written with this writer.
             *
             * @param facadeClassName JVM internal name of the corresponding multi-file class facade
             * @param metadataVersion metadata version to be written to the metadata (see [KotlinClassHeader.metadataVersion]),
             *   [KotlinClassMetadata.COMPATIBLE_METADATA_VERSION] by default. Cannot be less (lexicographically) than `[1, 4]`
             * @param extraInt the value of the class-level flags to be written to the metadata (see [KotlinClassHeader.extraInt]),
             *   0 by default
             */
            @Deprecated(
                "Writer API is deprecated. Please use KotlinClassMetadata.writeMultiFileClassPart(kmPackage, facadeClassName, metadataVersion, extraInt)",
                level = DeprecationLevel.ERROR
            )
            @JvmOverloads
            fun write(
                facadeClassName: String,
                metadataVersion: IntArray = COMPATIBLE_METADATA_VERSION,
                extraInt: Int = 0
            ): MultiFileClassPart {
                checkMetadataVersion(metadataVersion)
                val (d1, d2) = writeProtoBufData(t.build(), c)
                val metadata = Metadata(
                    MULTI_FILE_CLASS_PART_KIND, metadataVersion, d1, d2, facadeClassName, extraInt = extraInt
                )
                return MultiFileClassPart(metadata)
            }
        }
    }

    /**
     * Represents metadata of an unknown class file. This class is used if an old version of this library is used against a new kind
     * of class files generated by the Kotlin compiler, unsupported by this library.
     */
    class Unknown internal constructor(header: Metadata) : KotlinClassMetadata(header)

    companion object {
        // TODO: docs
        @OptIn(DeprecatedVisitor::class)
        @Suppress("DEPRECATION_ERROR")
        fun writeClass(
            kmClass: KmClass,
            metadataVersion: IntArray = COMPATIBLE_METADATA_VERSION,
            extraInt: Int = 0
        ): Class = KotlinClassMetadata.Class.Writer().also { kmClass.accept(it) }.write(metadataVersion, extraInt)

        // TODO: docs
        @OptIn(DeprecatedVisitor::class)
        @Suppress("DEPRECATION_ERROR")
        fun writeFileFacade(
            kmPackage: KmPackage,
            metadataVersion: IntArray = COMPATIBLE_METADATA_VERSION,
            extraInt: Int = 0
        ): FileFacade = FileFacade.Writer().also { kmPackage.accept(it) }.write(metadataVersion, extraInt)

        @OptIn(DeprecatedVisitor::class)
        @Suppress("DEPRECATION_ERROR")
        fun writeLambda(
            kmLambda: KmLambda,
            metadataVersion: IntArray = COMPATIBLE_METADATA_VERSION,
            extraInt: Int = 0
        ): SyntheticClass = SyntheticClass.Writer().also { kmLambda.accept(it) }.write(metadataVersion, extraInt)

        @OptIn(DeprecatedVisitor::class)
        @Suppress("DEPRECATION_ERROR")
        fun writeSyntheticClass(
            metadataVersion: IntArray = COMPATIBLE_METADATA_VERSION,
            extraInt: Int = 0
        ): SyntheticClass = SyntheticClass.Writer().write(metadataVersion, extraInt)

        @Suppress("DEPRECATION_ERROR")
        fun writeMultiFileClassFacade(
            partClassNames: List<String>, metadataVersion: IntArray = COMPATIBLE_METADATA_VERSION,
            extraInt: Int = 0
        ): MultiFileClassFacade = MultiFileClassFacade.Writer().write(partClassNames, metadataVersion, extraInt)

        @OptIn(DeprecatedVisitor::class)
        @Suppress("DEPRECATION_ERROR")
        fun writeMultiFileClassPart(
            kmPackage: KmPackage,
            facadeClassName: String,
            metadataVersion: IntArray = COMPATIBLE_METADATA_VERSION,
            extraInt: Int = 0
        ): MultiFileClassPart = MultiFileClassPart.Writer().also { kmPackage.accept(it) }.write(facadeClassName, metadataVersion, extraInt)


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
        fun read(header: Metadata): KotlinClassMetadata? {
            if (!JvmMetadataVersion(
                    header.metadataVersion,
                    (header.extraInt and (1 shl 3)/* see JvmAnnotationNames.METADATA_STRICT_VERSION_SEMANTICS_FLAG */) != 0
                ).isCompatible()
            ) return null

            return try {
                when (header.kind) {
                    CLASS_KIND -> Class(header)
                    FILE_FACADE_KIND -> FileFacade(header)
                    SYNTHETIC_CLASS_KIND -> SyntheticClass(header)
                    MULTI_FILE_CLASS_FACADE_KIND -> MultiFileClassFacade(header)
                    MULTI_FILE_CLASS_PART_KIND -> MultiFileClassPart(header)
                    else -> Unknown(header)
                }
            } catch (e: InconsistentKotlinMetadataException) {
                throw e
            } catch (e: Throwable) {
                throw InconsistentKotlinMetadataException("Exception occurred when reading Kotlin metadata", e)
            }
        }

        private fun checkMetadataVersion(version: IntArray) {
            require(version.size >= 2 && version[0] >= 1 && (version[0] > 1 || version[1] >= 4)) {
                "This version of kotlinx-metadata-jvm doesn't support writing Kotlin metadata of version earlier than 1.4. " +
                        "Please change the version from ${version.toList()} to at least [1, 4]."
            }
        }

        /**
         * A class file kind signifying that the corresponding class file contains a declaration of a Kotlin class.
         *
         * @see Metadata.kind
         */
        const val CLASS_KIND = 1

        /**
         * A class file kind signifying that the corresponding class file is a compiled Kotlin file facade.
         *
         * @see Metadata.kind
         */
        const val FILE_FACADE_KIND = 2

        /**
         * A class file kind signifying that the corresponding class file is synthetic, e.g. it's a class for lambda, `$DefaultImpls` class
         * for interface method implementations, `$WhenMappings` class for optimized `when` over enums, etc.
         *
         * @see Metadata.kind
         */
        const val SYNTHETIC_CLASS_KIND = 3

        /**
         * A class file kind signifying that the corresponding class file is a compiled multi-file class facade.
         *
         * @see Metadata.kind
         *
         * @see JvmMultifileClass
         */
        const val MULTI_FILE_CLASS_FACADE_KIND = 4

        /**
         * A class file kind signifying that the corresponding class file is a compiled multi-file class part, i.e. an internal class
         * with method bodies and their metadata, accessed only from the corresponding facade.
         *
         * @see Metadata.kind
         *
         * @see JvmMultifileClass
         */
        const val MULTI_FILE_CLASS_PART_KIND = 5

        /**
         * The latest metadata version supported by this version of the library.
         *
         * @see Metadata.metadataVersion
         */
        @JvmField // TODO: move it somewhere since it is also used in KotlinModuleMetadata?
        val COMPATIBLE_METADATA_VERSION = JvmMetadataVersion.INSTANCE.toArray().copyOf()
    }
}
