/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress(
    "DEPRECATION_ERROR", // Deprecated .accept implementation
    "DEPRECATION", // COMPATIBLE_METADATA_VERSION as default param
    "UNUSED_PARAMETER" // For deprecated Writer.write
)

package kotlin.metadata.jvm

import kotlin.metadata.*
import kotlin.metadata.internal.*
import kotlin.metadata.jvm.internal.*
import kotlin.metadata.jvm.internal.JvmReadUtils.readMetadataImpl
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion as CompilerMetadataVersion
import org.jetbrains.kotlin.metadata.jvm.serialization.JvmStringTable
import java.util.*

/**
 * Represents the parsed metadata of a Kotlin JVM class file. Entry point for parsing metadata on JVM.
 *
 * To create an instance of [KotlinClassMetadata], first obtain an instance of [Metadata] annotation on a class file,
 * and then call [KotlinClassMetadata.readStrict] or [KotlinClassMetadata.readLenient], depending on your application (see 'Working with different versions' section in the readme).
 *
 * [Metadata] annotation can be obtained either via reflection or created from data from a binary class file, using its constructor or helper function [kotlinx.metadata.jvm.Metadata].
 *
 * [KotlinClassMetadata] alone exposes only [KotlinClassMetadata.version] and [KotlinClassMetadata.flags];
 * to work with it, it is required to do a `when` over subclasses.
 * Different subclasses of [KotlinClassMetadata] represent different kinds of class files produced by Kotlin compiler.
 * It is recommended to study documentation for each subclass and decide what subclasses one has to handle in the `when` expression,
 * trying to cover as much as possible.
 * Normally, one would need at least a [Class] and a [FileFacade], as these are two most common kinds.
 *
 * Most of the subclasses declare a property to view metadata as a Km data structure — for example, [KotlinClassMetadata.Class.kmClass].
 * Some of them also can contain additional properties, e.g. [KotlinClassMetadata.MultiFileClassPart.facadeClassName].
 * Km data structures represent Kotlin declarations and offer a variety of properties to introspect and alter them.
 * After desired changes are made to a Km data structure or to [KotlinClassMetadata] itself,
 * it is possible to get a new raw metadata instance with a [write] function.
 *
 * > If metadata has been read with [readLenient], [write] will throw an exception.
 *
 * Here is an example of reading a content name of some metadata:
 * ```
 * fun displayName(metadata: Metadata): String = when (val kcm = KotlinClassMetadata.readStrict(metadata)) {
 *     is KotlinClassMetadata.Class -> "Class ${kcm.kmClass.name}"
 *     is KotlinClassMetadata.FileFacade -> "File facade with functions: ${kcm.kmPackage.functions.joinToString { it.name }}"
 *     is KotlinClassMetadata.SyntheticClass -> kcm.kmLambda?.function?.name?.let { "Lambda $it" } ?: "Synthetic class"
 *     is KotlinClassMetadata.MultiFileClassFacade -> "Multifile class facade with parts: ${kcm.partClassNames.joinToString()}"
 *     is KotlinClassMetadata.MultiFileClassPart -> "Multifile class part ${kcm.facadeClassName}"
 *     is KotlinClassMetadata.Unknown -> "Unknown metadata"
 * }
 * ```
 *
 * > In this example, only introspection (reading) is performed. In such cases, to support more broad range of metadata versions, you may also use [readLenient] method.
 */
public sealed class KotlinClassMetadata {

    /**
     * Encodes and writes this metadata to the new instance of [Metadata].
     *
     * This method encodes all available data, including [version] and [flags].
     * Due to technical limitations, it is not possible to write the metadata when the specified version is less than 1.4.
     *
     * @throws IllegalArgumentException if metadata is malformed or metadata was read in lenient mode and cannot be written back or [version] of this instance is less than 1.4.
     */
    public abstract fun write(): Metadata

    /**
     * Version of this metadata.
     */
    public abstract var version: JvmMetadataVersion

    /**
     * Additional classfile-level flags of this metadata. See [Metadata.extraInt] for possible values.
     */
    public abstract var flags: Int

    internal var isAllowedToWrite: Boolean = true

    /**
     * Represents metadata of a class file containing a declaration of a Kotlin class.
     *
     * Anything that does not belong to a Kotlin class (top-level declarations) is not present in
     * [Class] metadata, even if such declaration was in the same source file. See [FileFacade] for details.
     */
    public class Class public constructor(
        /**
         * [KmClass] representation of this metadata.
         *
         * Returns the same (mutable) [KmClass] instance every time.
         */
        public var kmClass: KmClass,
        public override var version: JvmMetadataVersion,
        public override var flags: Int,
    ) : KotlinClassMetadata() {

        internal constructor(annotationData: Metadata, lenient: Boolean) : this(
            JvmReadUtils.readKmClass(annotationData),
            JvmMetadataVersion(annotationData.metadataVersion),
            annotationData.extraInt
        ) {
            isAllowedToWrite = !lenient
        }

        override fun write(): Metadata {
            throwIfNotWriteable(isAllowedToWrite, "class")
            checkMetadataVersionForWrite(version)
            return wrapWriteIntoIAE {
                val writer = ClassWriter(JvmStringTable())
                writer.writeClass(kmClass)
                val (d1, d2) = writeProtoBufData(writer.t.build(), writer.c)
                Metadata(CLASS_KIND, version.toIntArray(), d1, d2, extraInt = flags)
            }
        }

        /**
         * Returns a new [KmClass] instance created from this class metadata.
         */
        @Deprecated(
            "To avoid excessive copying, use .kmClass property instead. Note that it returns a view and not a copy.",
            ReplaceWith("kmClass"),
            DeprecationLevel.ERROR
        )
        public fun toKmClass(): KmClass = KmClass().also { newKm -> kmClass.accept(newKm) } // defensive copy

        /**
         * Makes the given visitor visit the metadata of this class.
         *
         * @param v the visitor that must visit this class
         */
        @Deprecated(VISITOR_API_MESSAGE, level = DeprecationLevel.ERROR)
        public fun accept(v: KmClassVisitor): Unit = kmClass.accept(v)

        /**
         * A [KmClassVisitor] that generates the metadata of a Kotlin class.
         */
        @Deprecated(
            "$WRITER_API_MESSAGE, such as KotlinClassMetadata.writeClass(kmClass, metadataVersion, extraInt)",
            level = DeprecationLevel.ERROR
        )
        public class Writer : ClassWriter(JvmStringTable()) {
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
                level = DeprecationLevel.ERROR
            )
            public fun write(
                metadataVersion: IntArray = COMPATIBLE_METADATA_VERSION,
                extraInt: Int = 0
            ): Class {
                error("This method is no longer implemented. Migrate to KotlinClassMetadata.writeClass.")
            }
        }
    }

    /**
     * Represents metadata of a class file containing a compiled Kotlin file facade.
     *
     * File facade is a JVM class that contains declarations which do not belong to any Kotlin class: top-level functions, properties, and type aliases.
     * For example, file Main.kt that contains only `fun main()` would produce a `MainKt.class` with FileFacade with this function metadata.
     * If Kotlin source file contains both classes and top-level declarations, only top-level declarations would be available in the corresponding file facade.
     * Classes would have their own JVM classfiles and their own metadata of [Class] kind.
     */
    public class FileFacade public constructor(
        /**
         * [KmPackage] representation of this metadata.
         *
         * Returns the same (mutable) [KmPackage] instance every time.
         */
        public var kmPackage: KmPackage,
        /**
         * Version of this metadata.
         */
        public override var version: JvmMetadataVersion,
        /**
         * Additional classfile-level flags of this metadata. See [Metadata.extraInt] for possible values.
         */
        public override var flags: Int,
    ) : KotlinClassMetadata() {

        internal constructor(annotationData: Metadata, lenient: Boolean) : this(
            JvmReadUtils.readKmPackage(annotationData),
            JvmMetadataVersion(annotationData.metadataVersion),
            annotationData.extraInt
        ) {
            isAllowedToWrite = !lenient
        }

        override fun write(): Metadata {
            throwIfNotWriteable(isAllowedToWrite, "file facade")
            checkMetadataVersionForWrite(version)
            return wrapWriteIntoIAE {
                val writer = PackageWriter(JvmStringTable())
                writer.writePackage(kmPackage)
                val (d1, d2) = writeProtoBufData(writer.t.build(), writer.c)
                Metadata(FILE_FACADE_KIND, version.toIntArray(), d1, d2, extraInt = flags)
            }
        }

        /**
         * Creates a new [KmPackage] instance from this file facade metadata.
         */
        @Deprecated(
            "To avoid excessive copying, use .kmPackage property instead. Note that it returns a view and not a copy.",
            ReplaceWith("kmPackage"),
            DeprecationLevel.ERROR
        )
        public fun toKmPackage(): KmPackage = KmPackage().also { newPkg -> kmPackage.accept(newPkg) }

        /**
         * Makes the given visitor visit metadata of this file facade.
         *
         * @param v the visitor that must visit this file facade
         */
        @Deprecated(VISITOR_API_MESSAGE, level = DeprecationLevel.ERROR)
        public fun accept(v: KmPackageVisitor): Unit = kmPackage.accept(v)

        /**
         * A [KmPackageVisitor] that generates the metadata of a Kotlin file facade.
         */
        @Deprecated(
            "$WRITER_API_MESSAGE, such as KotlinClassMetadata.writeFileFacade(kmPackage, metadataVersion, extraInt)",
            level = DeprecationLevel.ERROR
        )
        public class Writer : PackageWriter(JvmStringTable()) {
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
                level = DeprecationLevel.ERROR
            )
            public fun write(
                metadataVersion: IntArray = COMPATIBLE_METADATA_VERSION,
                extraInt: Int = 0
            ): FileFacade {
                error("This method is no longer implemented. Migrate to KotlinClassMetadata.writeFileFacade.")
            }
        }
    }

    /**
     * Represents metadata of a class file containing a synthetic class, e.g. a class for lambda, `$DefaultImpls` class for interface
     * method implementations, `$WhenMappings` class for optimized `when` over enums, etc.
     */
    public class SyntheticClass(
        /**
         * [KmLambda] representation of this metadata, or `null` if this synthetic class does not represent a lambda.
         *
         * Returns the same (mutable) [KmLambda] instance every time.
         */
        public var kmLambda: KmLambda?,
        /**
         * Version of this metadata.
         */
        public override var version: JvmMetadataVersion,
        /**
         * Additional classfile-level flags of this metadata. See [Metadata.extraInt] for possible values.
         */
        public override var flags: Int,
    ) : KotlinClassMetadata() {

        internal constructor(annotationData: Metadata, lenient: Boolean) : this(
            JvmReadUtils.readKmLambda(annotationData),
            JvmMetadataVersion(annotationData.metadataVersion),
            annotationData.extraInt
        ) {
            isAllowedToWrite = !lenient
        }

        override fun write(): Metadata {
            throwIfNotWriteable(isAllowedToWrite, if (isLambda) "lambda" else "synthetic class")
            checkMetadataVersionForWrite(version)
            return if (isLambda) wrapWriteIntoIAE {
                val writer = LambdaWriter(JvmStringTable())
                writer.writeLambda(kmLambda!!)
                val proto = writer.t?.build()
                val (d1, d2) =
                    if (proto != null) writeProtoBufData(proto, writer.c)
                    else Pair(emptyArray<String>(), emptyArray<String>())
                Metadata(SYNTHETIC_CLASS_KIND, version.toIntArray(), d1, d2, extraInt = flags)
            } else {
                Metadata(SYNTHETIC_CLASS_KIND, version.toIntArray(), emptyArray<String>(), emptyArray<String>(), extraInt = flags)
            }
        }

        /**
         * Returns `true` if this synthetic class is a class file compiled for a Kotlin lambda.
         */
        public val isLambda: Boolean
            get() = kmLambda != null

        /**
         * Creates a new [KmLambda] instance from this synthetic class metadata.
         * Returns `null` if this synthetic class does not represent a lambda.
         */
        @Deprecated(
            "To avoid excessive copying, use .kmLambda property instead. Note that it returns a view and not a copy.",
            ReplaceWith("kmLambda"),
            DeprecationLevel.ERROR
        )
        public fun toKmLambda(): KmLambda? = if (isLambda) KmLambda().apply(this::accept) else null

        /**
         * Makes the given visitor visit metadata of this file facade if this synthetic class represents a Kotlin lambda
         * (`isLambda` == true).
         *
         * Throws [IllegalArgumentException] if this synthetic class does not represent a Kotlin lambda.
         *
         * @param v the visitor that must visit this lambda
         */
        @Deprecated(VISITOR_API_MESSAGE, level = DeprecationLevel.ERROR)
        public fun accept(v: KmLambdaVisitor) {
            if (!isLambda) throw IllegalArgumentException(
                "accept(KmLambdaVisitor) is only possible for synthetic classes which are lambdas (isLambda = true)"
            )

            kmLambda!!.accept(v)
        }

        /**
         * A [KmLambdaVisitor] that generates the metadata of a synthetic class. To generate metadata of a Kotlin lambda,
         * call [Writer.visitFunction] and [Writer.visitEnd] on a newly created instance of this writer. If these methods are not called,
         * the resulting metadata will represent a _non-lambda_ synthetic class.
         */
        @Deprecated(
            WRITER_API_MESSAGE + ": KotlinClassMetadata.writeLambda(kmLambda, metadataVersion, extraInt) " +
                    "or KotlinClassMetadata.writeSyntheticClass(metadataVersion, extraInt) for a non-lambda synthetic class",
            level = DeprecationLevel.ERROR
        )
        public class Writer : LambdaWriter(JvmStringTable()) {
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
                level = DeprecationLevel.ERROR
            )
            @JvmOverloads
            public fun write(
                metadataVersion: IntArray = COMPATIBLE_METADATA_VERSION,
                extraInt: Int = 0
            ): SyntheticClass {
                error("This method is no longer implemented. Migrate to KotlinClassMetadata.writeLambda or KotlinClassMetadata.writeSyntheticClass.")
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
    public class MultiFileClassFacade(
        /**
         * JVM internal names of the part classes which this multi-file class combines.
         */
        public var partClassNames: List<String>,
        /**
         * Version of this metadata.
         */
        public override var version: JvmMetadataVersion,
        /**
         * Additional classfile-level flags of this metadata. See [Metadata.extraInt] for possible values.
         */
        public override var flags: Int,
    ) : KotlinClassMetadata() {

        internal constructor(annotationData: Metadata, lenient: Boolean) : this(
            annotationData.data1.asList(),
            JvmMetadataVersion(annotationData.metadataVersion),
            annotationData.extraInt
        ) {
            isAllowedToWrite = !lenient
        }

        override fun write(): Metadata {
            throwIfNotWriteable(isAllowedToWrite, "multi-file class facade")
            checkMetadataVersionForWrite(version)
            return Metadata(
                MULTI_FILE_CLASS_FACADE_KIND, version.toIntArray(),
                partClassNames.toTypedArray<String>(), extraInt = flags
            )
        }

        /**
         * A writer that generates the metadata of a multi-file class facade.
         */
        @Deprecated(
            "$WRITER_API_MESSAGE, such as KotlinClassMetadata.writeMultiFileClassFacade(partClassNames, metadataVersion, extraInt)",
            level = DeprecationLevel.ERROR
        )
        public class Writer {
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
                level = DeprecationLevel.ERROR
            )
            @JvmOverloads
            public fun write(
                partClassNames: List<String>,
                metadataVersion: IntArray = COMPATIBLE_METADATA_VERSION,
                extraInt: Int = 0
            ): MultiFileClassFacade {
                error("This method is no longer implemented. Migrate to KotlinClassMetadata.writeMultiFileClassFacade.")
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
    public class MultiFileClassPart public constructor(
        /**
         * Returns the [KmPackage] representation of this metadata.
         *
         * Returns the same (mutable) [KmPackage] instance every time.
         */
        public var kmPackage: KmPackage,
        /**
         * JVM internal name of the corresponding multi-file class facade.
         */
        public var facadeClassName: String,
        /**
         * Version of this metadata.
         */
        public override var version: JvmMetadataVersion,
        /**
         * Additional classfile-level flags of this metadata. See [Metadata.extraInt] for possible values.
         */
        public override var flags: Int,
    ) : KotlinClassMetadata() {

        internal constructor(annotationData: Metadata, lenient: Boolean) : this(
            JvmReadUtils.readKmPackage(annotationData),
            annotationData.extraString,
            JvmMetadataVersion(annotationData.metadataVersion),
            annotationData.extraInt
        ) {
            isAllowedToWrite = !lenient
        }

        override fun write(): Metadata {
            throwIfNotWriteable(isAllowedToWrite, "multi-file class part")
            checkMetadataVersionForWrite(version)
            return wrapWriteIntoIAE {
                val writer = PackageWriter(JvmStringTable())
                writer.writePackage(kmPackage)
                val (d1, d2) = writeProtoBufData(writer.t.build(), writer.c)
                Metadata(
                    MULTI_FILE_CLASS_PART_KIND, version.toIntArray(), d1, d2, facadeClassName, extraInt = flags
                )
            }
        }

        /**
         * Creates a new [KmPackage] instance from this multi-file class part metadata.
         */
        @Deprecated(
            "To avoid excessive copying, use .kmPackage property instead. Note that it returns a view and not a copy.",
            ReplaceWith("kmPackage"),
            DeprecationLevel.ERROR
        )
        public fun toKmPackage(): KmPackage = KmPackage().also { newKmp -> kmPackage.accept(newKmp) }

        /**
         * Makes the given visitor visit metadata of this multi-file class part.
         *
         * @param v the visitor that must visit this multi-file class part
         */
        @Deprecated(VISITOR_API_MESSAGE, level = DeprecationLevel.ERROR)
        public fun accept(v: KmPackageVisitor) {
            kmPackage.accept(v)
        }

        /**
         * A [KmPackageVisitor] that generates the metadata of a multi-file class part.
         */
        @Deprecated(
            "$WRITER_API_MESSAGE, such as KotlinClassMetadata.writeMultiFileClassPart(kmPackage, facadeClassName, metadataVersion, extraInt)",
            level = DeprecationLevel.ERROR
        )
        public class Writer : PackageWriter(JvmStringTable()) {
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
                "$WRITER_API_MESSAGE, such as KotlinClassMetadata.writeMultiFileClassPart(kmPackage, facadeClassName, metadataVersion, extraInt)",
                level = DeprecationLevel.ERROR
            )
            @JvmOverloads
            public fun write(
                facadeClassName: String,
                metadataVersion: IntArray = COMPATIBLE_METADATA_VERSION,
                extraInt: Int = 0
            ): MultiFileClassPart {
                error("This method is no longer implemented. Migrate to KotlinClassMetadata.writeMultifileClassPart.")
            }
        }
    }

    /**
     * Represents metadata of an unknown class file. This class is used if an old version of this library is used against a new kind
     * of class files generated by the Kotlin compiler, unsupported by this library.
     */
    public class Unknown internal constructor(private val original: Metadata, private val lenient: Boolean) : KotlinClassMetadata() {
        /**
         * Version of this metadata.
         */
        public override var version: JvmMetadataVersion = JvmMetadataVersion(original.metadataVersion)

        /**
         * Additional classfile-level flags of this metadata. See [Metadata.extraInt] for possible values.
         */
        public override var flags: Int = original.extraInt

        override fun write(): Metadata {
            throwIfNotWriteable(!lenient, "unknown kind")
            checkMetadataVersionForWrite(version)
            return Metadata(
                original.kind,
                version.toIntArray(),
                original.data1,
                original.data2,
                original.extraString,
                original.packageName,
                flags
            )
        }

        @PublishedApi
        @Deprecated("This declaration is intended for binary compatibility only", level = DeprecationLevel.HIDDEN)
        internal companion object {
            @JvmField // For binary compatibility with previous `data object Unknown`
            public val INSTANCE: Unknown = Unknown(Metadata(kind = 99, metadataVersion = JvmMetadataVersion.LATEST_STABLE_SUPPORTED.toIntArray()), true)
        }
    }

    /**
     * Collection of methods for reading and writing [KotlinClassMetadata],
     * as well as metadata kind constants and [COMPATIBLE_METADATA_VERSION] constant.
     */
    public companion object {

        /**
         * Utility method to combine reading and writing of metadata:
         * First, [metadata] is parsed with [readStrict]; then, [transformer] is called on a read instance.
         * [transformer] may mutate passed instance of [KotlinClassMetadata] to achieve a desired result.
         * After transformation, [KotlinClassMetadata.write] method is called and its result becomes return value of this method.
         *
         * @throws IllegalArgumentException if metadata cannot be read or written
         *
         * @see readStrict
         * @see write
         */
        public fun transform(metadata: Metadata, transformer: (KotlinClassMetadata) -> Unit): Metadata {
            return readStrict(metadata).apply(transformer).write()
        }

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
        @JvmStatic
        @JvmOverloads
        @Deprecated("Use a KotlinClassMetadata.Class instance and its write() member function", level = DeprecationLevel.ERROR)
        public fun writeClass(
            kmClass: KmClass,
            metadataVersion: IntArray = COMPATIBLE_METADATA_VERSION,
            extraInt: Int = 0,
        ): Metadata = Class(kmClass, JvmMetadataVersion(metadataVersion), extraInt).write()


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
        @JvmStatic
        @JvmOverloads
        @Deprecated("Use a KotlinClassMetadata.FileFacade instance and its write() member function", level = DeprecationLevel.ERROR)
        public fun writeFileFacade(
            kmPackage: KmPackage,
            metadataVersion: IntArray = COMPATIBLE_METADATA_VERSION,
            extraInt: Int = 0,
        ): Metadata = FileFacade(kmPackage, JvmMetadataVersion(metadataVersion), extraInt).write()

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
        @JvmStatic
        @JvmOverloads
        @Deprecated("Use a KotlinClassMetadata.SyntheticClass instance and its write() member function", level = DeprecationLevel.ERROR)
        public fun writeLambda(
            kmLambda: KmLambda,
            metadataVersion: IntArray = COMPATIBLE_METADATA_VERSION,
            extraInt: Int = 0,
        ): Metadata = SyntheticClass(kmLambda, JvmMetadataVersion(metadataVersion), extraInt).write()

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
        @JvmStatic
        @JvmOverloads
        @Deprecated("Use a KotlinClassMetadata.SyntheticClass instance and its write() member function", level = DeprecationLevel.ERROR)
        public fun writeSyntheticClass(
            metadataVersion: IntArray = COMPATIBLE_METADATA_VERSION,
            extraInt: Int = 0,
        ): Metadata = SyntheticClass(null, JvmMetadataVersion(metadataVersion), extraInt).write()

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
        @JvmStatic
        @JvmOverloads
        @Deprecated(
            "Use a KotlinClassMetadata.MultiFileClassFacade instance and its write() member function",
            level = DeprecationLevel.ERROR
        )
        public fun writeMultiFileClassFacade(
            partClassNames: List<String>, metadataVersion: IntArray = COMPATIBLE_METADATA_VERSION,
            extraInt: Int = 0,
        ): Metadata = MultiFileClassFacade(partClassNames, JvmMetadataVersion(metadataVersion), extraInt).write()

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
        @JvmStatic
        @JvmOverloads
        @Deprecated(
            "Use a KotlinClassMetadata.MultiFileClassPart instance and its write() member function",
            level = DeprecationLevel.ERROR
        )
        public fun writeMultiFileClassPart(
            kmPackage: KmPackage,
            facadeClassName: String,
            metadataVersion: IntArray = COMPATIBLE_METADATA_VERSION,
            extraInt: Int = 0,
        ): Metadata = MultiFileClassPart(kmPackage, facadeClassName, JvmMetadataVersion(metadataVersion), extraInt).write()

        /**
         * Reads and parses the given annotation data of a Kotlin JVM class file and returns the correct type of [KotlinClassMetadata] encoded by
         * this annotation, if metadata version is supported.
         *
         * [annotationData] may be obtained reflectively, constructed manually or with helper [kotlinx.metadata.jvm.Metadata] function,
         * or equivalent [KotlinClassHeader] can be used.
         *
         * Metadata version is supported if it is greater or equal than 1.1, and less or equal than [COMPATIBLE_METADATA_VERSION] + 1 minor version.
         * Note that metadata version is 1.1 for Kotlin < 1.4, and is equal to the language version starting from Kotlin 1.4.
         * For example, if the latest Kotlin version is 1.7.0, the latest kotlinx-metadata-jvm can read binaries produced by Kotlin
         * compilers from 1.0 to 1.8.* inclusively.
         *
         * @throws IllegalArgumentException if the metadata version is unsupported
         *
         * @see COMPATIBLE_METADATA_VERSION
         */
        @JvmStatic
        @Deprecated(
            "read() throws an error if metadata version is too high. Use either readStrict() if you want to retain this behavior, or readLenient() if you want to try to read newer metadata.",
            ReplaceWith("KotlinClassMetadata.readStrict(annotationData)"),
            DeprecationLevel.ERROR
        )
        public fun read(annotationData: Metadata): KotlinClassMetadata = readMetadataImpl(annotationData, lenient = false)

        /**
         * Reads and parses the given annotation data of a Kotlin JVM class file and returns the correct type of [KotlinClassMetadata] encoded by
         * this annotation, if the metadata version is supported.
         *
         * [annotationData] may be obtained reflectively, constructed manually or with helper [kotlinx.metadata.jvm.Metadata] function,
         * or equivalent [KotlinClassHeader] can be used.
         *
         * This method can read only supported metadata versions (see [JvmMetadataVersion.LATEST_STABLE_SUPPORTED] for definition).
         * It will throw an exception if the metadata version is greater than what kotlinx-metadata-jvm understands.
         * It is suitable when your tooling cannot tolerate reading potentially incomplete or incorrect information due to version differences.
         * It is also the only method that allows metadata transformation and `KotlinClassMetadata.write` subsequent calls.
         *
         * @throws IllegalArgumentException if the metadata version is unsupported or if metadata is corrupted
         *
         * @see JvmMetadataVersion.LATEST_STABLE_SUPPORTED
         */
        @JvmStatic
        public fun readStrict(annotationData: Metadata): KotlinClassMetadata = readMetadataImpl(annotationData, lenient = false)

        /**
         * Reads and parses the given annotation data of a Kotlin JVM class file and returns the correct type of [KotlinClassMetadata] encoded by
         * this annotation. [KotlinClassMetadata] instances obtained from this method cannot be written.
         *
         * [annotationData] may be obtained reflectively, constructed manually or with helper [kotlinx.metadata.jvm.Metadata] function,
         * or equivalent [KotlinClassHeader] can be used.
         *
         * This method makes best effort to read unsupported metadata versions.
         * If metadata version is greater than [JvmMetadataVersion.LATEST_STABLE_SUPPORTED] + 1, this method still attempts to read it and may ignore parts of the metadata it does not understand.
         * Keep in mind that this method will still throw an exception if metadata is changed in an unpredictable way.
         * Because obtained metadata can be incomplete, its [KotlinClassMetadata.write] method will throw an exception.
         * This method still cannot read metadata produced by pre-1.0 compilers.
         *
         * @throws IllegalArgumentException if the metadata version is that of Kotlin 1.0 or metadata format has been changed in an unpredictable way and reading of incompatible metadata is not possible
         *
         * @see JvmMetadataVersion.LATEST_STABLE_SUPPORTED
         */
        @JvmStatic
        public fun readLenient(annotationData: Metadata): KotlinClassMetadata = readMetadataImpl(annotationData, lenient = true)

        internal fun throwIfNotWriteable(writeable: Boolean, name: String) {
            if (writeable) return
            throw IllegalArgumentException("This $name cannot be written because it represents metadata read in lenient mode")
        }

        private fun checkMetadataVersionForWrite(version: JvmMetadataVersion) {
            require(version.major >= 1 && (version.major > 1 || version.minor >= 4)) {
                "This version of kotlinx-metadata-jvm doesn't support writing Kotlin metadata of version earlier than 1.4. " +
                        "Please change the version from $version to at least [1, 4]."
            }
            require(version <= JvmMetadataVersion.HIGHEST_ALLOWED_TO_WRITE) {
                "kotlinx-metadata-jvm cannot write metadata for future compiler versions. Requested to write version $version, but highest known version is ${JvmMetadataVersion.HIGHEST_ALLOWED_TO_WRITE}"
            }
        }

        /**
         * A class file kind signifying that the corresponding class file contains a declaration of a Kotlin class.
         *
         * @see Metadata.kind
         */
        public const val CLASS_KIND: Int = 1

        /**
         * A class file kind signifying that the corresponding class file is a compiled Kotlin file facade.
         *
         * @see Metadata.kind
         */
        public const val FILE_FACADE_KIND: Int = 2

        /**
         * A class file kind signifying that the corresponding class file is synthetic, e.g. it is a class for lambda, `$DefaultImpls` class
         * for interface method implementations, `$WhenMappings` class for optimized `when` over enums, etc.
         *
         * @see Metadata.kind
         */
        public const val SYNTHETIC_CLASS_KIND: Int = 3

        /**
         * A class file kind signifying that the corresponding class file is a compiled multi-file class facade.
         *
         * @see Metadata.kind
         *
         * @see JvmMultifileClass
         */
        public const val MULTI_FILE_CLASS_FACADE_KIND: Int = 4

        /**
         * A class file kind signifying that the corresponding class file is a compiled multi-file class part, i.e. an internal class
         * with method bodies and their metadata, accessed only from the corresponding facade.
         *
         * @see Metadata.kind
         *
         * @see JvmMultifileClass
         */
        public const val MULTI_FILE_CLASS_PART_KIND: Int = 5

        /**
         * The latest stable metadata version supported by this version of the library.
         * The library can read in strict mode Kotlin metadata produced by Kotlin compilers from 1.0 up to and including this version + 1 minor.
         *
         * In other words, a metadata version is supported if it is greater or equal than 1.1, and less or equal than [COMPATIBLE_METADATA_VERSION] + 1 minor version.
         * Note that a metadata version is 1.1 for Kotlin < 1.4, and is equal to the language version starting from Kotlin 1.4.
         *
         * For example, if the latest supported stable Kotlin version is 1.7.0, kotlinx-metadata-jvm can read binaries produced by Kotlin compilers from 1.0
         * to 1.8.* inclusively. In this case, this property will have the value `[1, 7, 0]`.
         *
         * @see Metadata.metadataVersion
         */
        @JvmField
        @Deprecated("Use JvmMetadataVersion.LATEST_STABLE_SUPPORTED instead", ReplaceWith("JvmMetadataVersion.LATEST_STABLE_SUPPORTED"), DeprecationLevel.ERROR)
        public val COMPATIBLE_METADATA_VERSION: IntArray = CompilerMetadataVersion.INSTANCE.toArray().copyOf()

    }
}

internal const val VISITOR_API_MESSAGE =
    "Visitor API is deprecated as excessive and cumbersome. Please use nodes (such as KmClass) and their properties."

internal const val WRITER_API_MESSAGE =
    "Visitor Writer API is deprecated as excessive and cumbersome. Please use member functions of KotlinClassMetadata.Companion"
