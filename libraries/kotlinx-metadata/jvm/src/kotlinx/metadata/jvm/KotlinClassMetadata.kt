/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("DEPRECATION")

package kotlinx.metadata.jvm

import kotlinx.metadata.*
import kotlinx.metadata.internal.*
import kotlinx.metadata.jvm.internal.*
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.metadata.jvm.serialization.JvmStringTable
import java.util.*
import kotlin.LazyThreadSafetyMode.PUBLICATION

/**
 * Represents the parsed metadata of a Kotlin JVM class file.
 *
 * To create an instance of [KotlinClassMetadata], first obtain an instance of [Metadata] annotation on a class file, and then call [KotlinClassMetadata.read].
 * [Metadata] annotation can be obtained either via reflection or created from data from a binary class file, using its constructor or helper function [kotlinx.metadata.jvm.Metadata].
 *
 * [KotlinClassMetadata] itself does not have any meaningful methods or properties; to work with it, it is required to do a `when` over subclasses.
 * Different subclasses of [KotlinClassMetadata] represent different kinds of class files produced by Kotlin compiler.
 * It is recommended to study documentation for each subclass and decide what subclasses one has to handle in the `when` expression,
 * trying to cover as much as possible.
 * Normally, one would need at least a [Class] and a [FileFacade], as these are two most common kinds.
 *
 * Most of the subclasses offer a conversion method to transform metadata into a Km data structure — for example, [KotlinClassMetadata.Class.toKmClass].
 * Km data structures represent Kotlin declarations and offer variety of properties to introspect and alter them.
 * Note that parsing may be lazy, depending on the implementation; therefore, one may not see an [IllegalArgumentException] indicating malformed metadata
 * until one calls `toKmClass` or a similar method.
 */
sealed class KotlinClassMetadata(val annotationData: Metadata) {

    /**
     * Represents metadata of a class file containing a declaration of a Kotlin class.
     *
     * Anything that does not belong to a Kotlin class (top-level declarations) is not present in
     * [Class] metadata, even if such declaration was in the same source file. See [FileFacade] for details.
     */
    class Class internal constructor(annotationData: Metadata) : KotlinClassMetadata(annotationData) {
        private val classData by lazy(PUBLICATION) {
            JvmProtoBufUtil.readClassDataFrom(annotationData.requireNotEmpty(), annotationData.data2)
        }

        /**
         * Returns a new [KmClass] instance created from this class metadata.
         *
         * @throws IllegalArgumentException if metadata is malformed and can't be transformed into [KmClass].
         */
        fun toKmClass(): KmClass = wrapIntoMetadataExceptionWhenNeeded {
            KmClass().apply(this::accept)
        }

        /**
         * Makes the given visitor visit metadata of this class.
         *
         * @param v the visitor that must visit this class
         */
        @Deprecated(VISITOR_API_MESSAGE)
        fun accept(v: KmClassVisitor) {
            val (strings, proto) = classData
            proto.accept(v, strings)
        }

        /**
         * A [KmClassVisitor] that generates the metadata of a Kotlin class.
         */
        @Deprecated(
            "$WRITER_API_MESSAGE, such as KotlinClassMetadata.writeClass(kmClass, metadataVersion, extraInt)",
        )
        class Writer : ClassWriter(JvmStringTable()) {
            /**
             * Returns the metadata of the class that was written with this writer.
             *
             * @param metadataVersion metadata version to be written to the metadata (see [Metadata.metadataVersion]),
             *   [KotlinClassMetadata.COMPATIBLE_METADATA_VERSION] by default. Cannot be less (lexicographically) than `[1, 4]`
             * @param extraInt the value of the class-level flags to be written to the metadata (see [Metadata.extraInt]),
             *   0 by default
             */
            @JvmOverloads
            @Deprecated(
                "$WRITER_API_MESSAGE, such as KotlinClassMetadata.writeClass(kmClass, metadataVersion, extraInt)",
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
     *
     * File facade is a JVM class that contains declarations which do not belong to any Kotlin class: top-level functions, properties and type aliases.
     * For example, file Main.kt that contains only `fun main()` would produce a `MainKt.class` with FileFacade with this function metadata.
     * If Kotlin source file contains both classes and top-level declarations, only top-level declarations would be available in the corresponding file facade.
     * Classes would have their own JVM classfiles and their own metadata of [Class] kind.
     */
    class FileFacade internal constructor(annotationData: Metadata) : KotlinClassMetadata(annotationData) {
        private val packageData by lazy(PUBLICATION) {
            JvmProtoBufUtil.readPackageDataFrom(annotationData.requireNotEmpty(), annotationData.data2)
        }

        /**
         * Creates a new [KmPackage] instance from this file facade metadata.
         *
         * @throws IllegalArgumentException if metadata is malformed and can't be transformed into [KmPackage].
         */
        fun toKmPackage(): KmPackage = wrapIntoMetadataExceptionWhenNeeded {
            KmPackage().apply(this::accept)
        }

        /**
         * Makes the given visitor visit metadata of this file facade.
         *
         * @param v the visitor that must visit this file facade
         */
        @Deprecated(VISITOR_API_MESSAGE)
        fun accept(v: KmPackageVisitor) {
            val (strings, proto) = packageData
            proto.accept(v, strings)
        }

        /**
         * A [KmPackageVisitor] that generates the metadata of a Kotlin file facade.
         */
        @Deprecated(
            "$WRITER_API_MESSAGE, such as KotlinClassMetadata.writeFileFacade(kmPackage, metadataVersion, extraInt)",
        )
        class Writer : PackageWriter(JvmStringTable()) {
            /**
             * Returns the metadata of the file facade that was written with this writer.
             *
             * @param metadataVersion metadata version to be written to the metadata (see [Metadata.metadataVersion]),
             *   [KotlinClassMetadata.COMPATIBLE_METADATA_VERSION] by default. Cannot be less (lexicographically) than `[1, 4]`
             * @param extraInt the value of the class-level flags to be written to the metadata (see [Metadata.extraInt]),
             *   0 by default
             */
            @JvmOverloads
            @Deprecated(
                "$WRITER_API_MESSAGE, such as KotlinClassMetadata.writeFileFacade(kmPackage, metadataVersion, extraInt)",
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
    class SyntheticClass internal constructor(annotationData: Metadata) : KotlinClassMetadata(annotationData) {
        private val functionData by lazy(PUBLICATION) {
            annotationData.data1.takeIf(Array<*>::isNotEmpty)?.let { data1 ->
                JvmProtoBufUtil.readFunctionDataFrom(data1, annotationData.data2)
            }
        }

        /**
         * Creates a new [KmLambda] instance from this synthetic class metadata.
         * Returns `null` if this synthetic class does not represent a lambda.
         *
         * @throws IllegalArgumentException if metadata is malformed and can't be transformed into [KmLambda].
         */
        fun toKmLambda(): KmLambda? = wrapIntoMetadataExceptionWhenNeeded {
            if (isLambda) KmLambda().apply(this::accept) else null
        }

        /**
         * Returns `true` if this synthetic class is a class file compiled for a Kotlin lambda.
         */
        val isLambda: Boolean
            get() = annotationData.data1.isNotEmpty()

        /**
         * Makes the given visitor visit metadata of this file facade, if this synthetic class represents a Kotlin lambda
         * (`isLambda` == true).
         *
         * Throws [IllegalArgumentException] if this synthetic class does not represent a Kotlin lambda.
         *
         * @param v the visitor that must visit this lambda
         */
        @Deprecated(VISITOR_API_MESSAGE)
        fun accept(v: KmLambdaVisitor) {
            if (!isLambda) throw IllegalArgumentException(
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
        @Deprecated(
            WRITER_API_MESSAGE + ": KotlinClassMetadata.writeLambda(kmLambda, metadataVersion, extraInt) " +
                    "or KotlinClassMetadata.writeSyntheticClass(metadataVersion, extraInt) for a non-lambda synthetic class",
        )
        class Writer : LambdaWriter(JvmStringTable()) {
            /**
             * Returns the metadata of the synthetic class that was written with this writer.
             *
             * @param metadataVersion metadata version to be written to the metadata (see [Metadata.metadataVersion]),
             *   [KotlinClassMetadata.COMPATIBLE_METADATA_VERSION] by default. Cannot be less (lexicographically) than `[1, 4]`
             * @param extraInt the value of the class-level flags to be written to the metadata (see [Metadata.extraInt]),
             *   0 by default
             */
            @Deprecated(
                WRITER_API_MESSAGE + ": KotlinClassMetadata.writeLambda(kmLambda, metadataVersion, extraInt) " +
                        "or KotlinClassMetadata.writeSyntheticClass(metadataVersion, extraInt) for a non-lambda synthetic class",
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
     * Multi-file class facade is a facade file produced from several Kotlin source files marked with [JvmMultifileClass] and same [JvmName].
     * It does not have any declarations; it only contains [partClassNames] to indicate where individual parts are located.
     *
     * Consider the following example.
     * Suppose we have two files, partOne.kt and partTwo.kt:
     *
     * ```
     * // partOne.kt
     * @file:JvmMultifileClass
     * @file:JvmName("MultiFileClass")
     *
     * fun partOne(): String = "one"
     *
     * // partTwo.kt
     * @file:JvmMultifileClass
     * @file:JvmName("MultiFileClass")
     *
     * fun partTwo(): String = "two"
     * ```
     *
     * In this case, there would be three classfiles produced by the compiler, each with its own metadata.
     * Metadata for `MultiFileClass.class` would be of type [MultiFileClassFacade]
     * and contain [partClassNames] that would indicate class file names for the parts: `[MultiFileClass__PartOneKt, MultiFileClass__PartTwoKt]`.
     * Using these names, you can load metadata from those classes with type [MultiFileClassPart].
     *
     * @see MultiFileClassPart
     * @see JvmMultifileClass
     */
    class MultiFileClassFacade internal constructor(annotationData: Metadata) : KotlinClassMetadata(annotationData) {
        /**
         * JVM internal names of the part classes which this multi-file class combines.
         */
        val partClassNames: List<String> = annotationData.data1.asList()

        /**
         * A writer that generates the metadata of a multi-file class facade.
         */
        @Deprecated(
            "$WRITER_API_MESSAGE, such as KotlinClassMetadata.writeMultiFileClassFacade(partClassNames, metadataVersion, extraInt)",
        )
        class Writer {
            /**
             * Returns the metadata of the multi-file class facade that was written with this writer.
             *
             * @param partClassNames JVM internal names of the part classes which this multi-file class combines
             * @param metadataVersion metadata version to be written to the metadata (see [Metadata.metadataVersion]),
             *   [KotlinClassMetadata.COMPATIBLE_METADATA_VERSION] by default. Cannot be less (lexicographically) than `[1, 4]`
             * @param extraInt the value of the class-level flags to be written to the metadata (see [Metadata.extraInt]),
             *   0 by default
             */
            @Deprecated(
                "$WRITER_API_MESSAGE, such as KotlinClassMetadata.writeMultiFileClassFacade(partClassNames, metadataVersion, extraInt)",
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
     * and their metadata, accessed only from the corresponding [facade][MultiFileClassFacade]. Just like [FileFacade], this metadata contains only top-level declarations,
     * as classes have their own one.
     *
     * It does not contain any references to other parts; to locate all the parts, one should construct a corresponding
     * [facade][MultiFileClassFacade] beforehand.
     *
     * @see MultiFileClassFacade
     * @see JvmMultifileClass
     */
    class MultiFileClassPart internal constructor(annotationData: Metadata) : KotlinClassMetadata(annotationData) {
        private val packageData by lazy(PUBLICATION) {
            JvmProtoBufUtil.readPackageDataFrom(annotationData.requireNotEmpty(), annotationData.data2)
        }

        /**
         * JVM internal name of the corresponding multi-file class facade.
         */
        val facadeClassName: String
            get() = annotationData.extraString

        /**
         * Creates a new [KmPackage] instance from this multi-file class part metadata.
         *
         * @throws IllegalArgumentException if metadata is malformed and can't be transformed into [KmPackage].
         */
        fun toKmPackage(): KmPackage = wrapIntoMetadataExceptionWhenNeeded {
            KmPackage().apply(this::accept)
        }

        /**
         * Makes the given visitor visit metadata of this multi-file class part.
         *
         * @param v the visitor that must visit this multi-file class part
         */
        @Deprecated(VISITOR_API_MESSAGE)
        fun accept(v: KmPackageVisitor) {
            val (strings, proto) = packageData
            proto.accept(v, strings)
        }

        /**
         * A [KmPackageVisitor] that generates the metadata of a multi-file class part.
         */
        @Deprecated(
            "$WRITER_API_MESSAGE, such as KotlinClassMetadata.writeMultiFileClassPart(kmPackage, facadeClassName, metadataVersion, extraInt)"
        )
        class Writer : PackageWriter(JvmStringTable()) {
            /**
             * Returns the metadata of the multi-file class part that was written with this writer.
             *
             * @param facadeClassName JVM internal name of the corresponding multi-file class facade
             * @param metadataVersion metadata version to be written to the metadata (see [Metadata.metadataVersion]),
             *   [KotlinClassMetadata.COMPATIBLE_METADATA_VERSION] by default. Cannot be less (lexicographically) than `[1, 4]`
             * @param extraInt the value of the class-level flags to be written to the metadata (see [Metadata.extraInt]),
             *   0 by default
             */
            @Deprecated(
                "$WRITER_API_MESSAGE, such as KotlinClassMetadata.writeMultiFileClassPart(kmPackage, facadeClassName, metadataVersion, extraInt)"
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
    class Unknown internal constructor(annotationData: Metadata) : KotlinClassMetadata(annotationData)

    companion object {
        /**
         * Writes contents of [kmClass] as the class metadata.
         *
         * @param metadataVersion metadata version to be written to the metadata (see [Metadata.metadataVersion]),
         *   [KotlinClassMetadata.COMPATIBLE_METADATA_VERSION] by default. Cannot be less (lexicographically) than `[1, 4]`
         * @param extraInt the value of the class-level flags to be written to the metadata (see [Metadata.extraInt]),
         *   0 by default
         *
         * @throws IllegalArgumentException if [kmClass] is not correct and cannot be written or if [metadataVersion] is not supported for writing.
         */
        fun writeClass(
            kmClass: KmClass,
            metadataVersion: IntArray = COMPATIBLE_METADATA_VERSION,
            extraInt: Int = 0
        ): Class = wrapWriteIntoIAE {
            Class.Writer().also { kmClass.accept(it) }.write(metadataVersion, extraInt)
        }

        /**
         * Writes [kmPackage] contents as the file facade metadata.
         *
         * @param metadataVersion metadata version to be written to the metadata (see [Metadata.metadataVersion]),
         *   [KotlinClassMetadata.COMPATIBLE_METADATA_VERSION] by default. Cannot be less (lexicographically) than `[1, 4]`
         * @param extraInt the value of the class-level flags to be written to the metadata (see [Metadata.extraInt]),
         *   0 by default
         *
         * @throws IllegalArgumentException if [kmPackage] is not correct and cannot be written or if [metadataVersion] is not supported for writing.
         */
        fun writeFileFacade(
            kmPackage: KmPackage,
            metadataVersion: IntArray = COMPATIBLE_METADATA_VERSION,
            extraInt: Int = 0
        ): FileFacade = wrapWriteIntoIAE {
            FileFacade.Writer().also { kmPackage.accept(it) }.write(metadataVersion, extraInt)
        }

        /**
         * Writes [kmLambda] as the synthetic class metadata.
         *
         * @param metadataVersion metadata version to be written to the metadata (see [Metadata.metadataVersion]),
         *   [KotlinClassMetadata.COMPATIBLE_METADATA_VERSION] by default. Cannot be less (lexicographically) than `[1, 4]`
         * @param extraInt the value of the class-level flags to be written to the metadata (see [Metadata.extraInt]),
         *   0 by default
         *
         * @throws IllegalArgumentException if [kmLambda] is not correct and cannot be written or if [metadataVersion] is not supported for writing.
         */
        fun writeLambda(
            kmLambda: KmLambda,
            metadataVersion: IntArray = COMPATIBLE_METADATA_VERSION,
            extraInt: Int = 0
        ): SyntheticClass = wrapWriteIntoIAE {
            SyntheticClass.Writer().also { kmLambda.accept(it) }.write(metadataVersion, extraInt)
        }

        /**
         * Writes synthetic class metadata.
         *
         * @param metadataVersion metadata version to be written to the metadata (see [Metadata.metadataVersion]),
         *   [KotlinClassMetadata.COMPATIBLE_METADATA_VERSION] by default. Cannot be less (lexicographically) than `[1, 4]`
         * @param extraInt the value of the class-level flags to be written to the metadata (see [Metadata.extraInt]),
         *   0 by default
         *
         * @throws IllegalArgumentException if [metadataVersion] is not supported for writing.
         */
        fun writeSyntheticClass(
            metadataVersion: IntArray = COMPATIBLE_METADATA_VERSION,
            extraInt: Int = 0
        ): SyntheticClass = SyntheticClass.Writer().write(metadataVersion, extraInt)

        /**
         * Writes metadata of the multi-file class facade.
         *
         * @param partClassNames JVM internal names of the part classes which this multi-file class combines
         * @param metadataVersion metadata version to be written to the metadata (see [Metadata.metadataVersion]),
         *   [KotlinClassMetadata.COMPATIBLE_METADATA_VERSION] by default. Cannot be less (lexicographically) than `[1, 4]`
         * @param extraInt the value of the class-level flags to be written to the metadata (see [Metadata.extraInt]),
         *   0 by default
         *
         * @throws IllegalArgumentException if [metadataVersion] is not supported for writing.
         */
        fun writeMultiFileClassFacade(
            partClassNames: List<String>, metadataVersion: IntArray = COMPATIBLE_METADATA_VERSION,
            extraInt: Int = 0
        ): MultiFileClassFacade = MultiFileClassFacade.Writer().write(partClassNames, metadataVersion, extraInt)

        /**
         * Writes the metadata of the multi-file class part.
         *
         * @param facadeClassName JVM internal name of the corresponding multi-file class facade
         * @param metadataVersion metadata version to be written to the metadata (see [Metadata.metadataVersion]),
         *   [KotlinClassMetadata.COMPATIBLE_METADATA_VERSION] by default. Cannot be less (lexicographically) than `[1, 4]`
         * @param extraInt the value of the class-level flags to be written to the metadata (see [Metadata.extraInt]),
         *   0 by default
         *
         * @throws IllegalArgumentException if [kmPackage] is not correct and cannot be written or if [metadataVersion] is not supported for writing.
         */
        fun writeMultiFileClassPart(
            kmPackage: KmPackage,
            facadeClassName: String,
            metadataVersion: IntArray = COMPATIBLE_METADATA_VERSION,
            extraInt: Int = 0
        ): MultiFileClassPart = wrapWriteIntoIAE {
            MultiFileClassPart.Writer().also { kmPackage.accept(it) }.write(facadeClassName, metadataVersion, extraInt)
        }

        /**
         * Reads and parses the given annotation data of a Kotlin JVM class file and returns the correct type of [KotlinClassMetadata] encoded by
         * this annotation, or `null` if this annotation data has an unsupported metadata version.
         *
         * [annotationData] may be obtained reflectively, constructed manually or with helper [kotlinx.metadata.jvm.Metadata] function,
         * or equivalent [KotlinClassHeader] can be used.
         *
         * @throws IllegalArgumentException if the metadata cannot be parsed from binary format or is inconsistent with itself
         */
        @JvmStatic
        fun read(annotationData: Metadata): KotlinClassMetadata? {
            if (!JvmMetadataVersion(
                    annotationData.metadataVersion,
                    (annotationData.extraInt and (1 shl 3)/* see JvmAnnotationNames.METADATA_STRICT_VERSION_SEMANTICS_FLAG */) != 0
                ).isCompatibleWithCurrentCompilerVersion()
            ) return null

            // All data is loaded lazily, no exceptions here to handle
            return when (annotationData.kind) {
                CLASS_KIND -> Class(annotationData)
                FILE_FACADE_KIND -> FileFacade(annotationData)
                SYNTHETIC_CLASS_KIND -> SyntheticClass(annotationData)
                MULTI_FILE_CLASS_FACADE_KIND -> MultiFileClassFacade(annotationData)
                MULTI_FILE_CLASS_PART_KIND -> MultiFileClassPart(annotationData)
                else -> Unknown(annotationData)
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

internal const val VISITOR_API_MESSAGE =
    "Visitor API is deprecated as excessive and cumbersome. Please use nodes (such as KmClass) and their properties."

internal const val WRITER_API_MESSAGE =
    "Visitor Writer API is deprecated as excessive and cumbersome. Please use member functions of KotlinClassMetadata.Companion"