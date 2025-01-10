/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.metadata.jvm

import kotlin.metadata.*
import kotlin.metadata.internal.*
import kotlin.metadata.jvm.internal.*
import kotlin.metadata.jvm.internal.JvmReadUtils.readMetadataImpl
import org.jetbrains.kotlin.metadata.jvm.serialization.JvmStringTable

/**
 * Represents the parsed metadata of a Kotlin JVM class file. Entry point for parsing metadata on JVM.
 *
 * To create an instance of [KotlinClassMetadata], first obtain an instance of [Metadata] annotation on a class file,
 * and then call [KotlinClassMetadata.readStrict] or [KotlinClassMetadata.readLenient], depending on your application (see 'Working with different versions' section in the readme).
 *
 * [Metadata] annotation can be obtained either via reflection or created from data from a binary class file, using its constructor or helper function [kotlin.metadata.jvm.Metadata].
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
 * > In this example, only introspection (reading) is performed. In such cases, to support broader range of metadata versions, you may also use [readLenient] method.
 */
public sealed class KotlinClassMetadata {

    /**
     * Encodes and writes this metadata to the new instance of [Metadata].
     *
     * This method encodes all available data, including [version] and [flags].
     * Due to technical limitations, it is not possible to write the metadata when the specified version is less than 1.4.
     * It is also not possible to write metadata with the version higher that [JvmMetadataVersion.LATEST_STABLE_SUPPORTED] + 1, as
     * we do not know if a metadata format is the same for yet unreleased Kotlin compilers.
     *
     * @throws IllegalArgumentException if metadata is malformed, or metadata was read in lenient mode and cannot be written back,
     * or [version] of this instance is less than 1.4, or [version] of this instance is too high.
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
    }

    /**
     * Represents metadata of a class file containing a synthetic class, e.g., a class for lambda, `$DefaultImpls` class for interface
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
    }

    /**
     * Represents metadata of a class file containing a compiled multi-file class part, i.e., an internal class with method bodies
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
    }

    /**
     * Collection of methods for reading and writing [KotlinClassMetadata],
     * as well as metadata kind constants.
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
         * Reads and parses the given annotation data of a Kotlin JVM class file and returns the correct type of [KotlinClassMetadata] encoded by
         * this annotation, if the metadata version is supported.
         *
         * [annotationData] may be obtained reflectively, constructed manually or with helper [kotlin.metadata.jvm.Metadata] function,
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
         * [annotationData] may be obtained reflectively, constructed manually or with helper [kotlin.metadata.jvm.Metadata] function,
         * or equivalent [KotlinClassHeader] can be used.
         *
         * This method makes best effort to read unsupported metadata versions.
         * If [annotationData] version is greater than [JvmMetadataVersion.LATEST_STABLE_SUPPORTED] + 1, this method still attempts to read it and may ignore parts of the metadata it does not understand.
         * Keep in mind that this method will still throw an exception if metadata is changed in an unpredictable way.
         * Because obtained metadata can be incomplete, its [KotlinClassMetadata.write] method will throw an exception.
         * This method still cannot read metadata produced by pre-1.0 compilers.
         *
         * @throws IllegalArgumentException if the metadata version is that of Kotlin 1.0, or the metadata format has been changed in an unpredictable way and reading of incompatible metadata is not possible
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
         * A class file kind signifying that the corresponding class file is synthetic, e.g., it is a class for lambda, `$DefaultImpls` class
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
         * A class file kind signifying that the corresponding class file is a compiled multi-file class part, i.e., an internal class
         * with method bodies and their metadata, accessed only from the corresponding facade.
         *
         * @see Metadata.kind
         *
         * @see JvmMultifileClass
         */
        public const val MULTI_FILE_CLASS_PART_KIND: Int = 5
    }
}
