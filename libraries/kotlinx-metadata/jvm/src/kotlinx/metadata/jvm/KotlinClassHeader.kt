/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.jvm

import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmBytecodeBinaryVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion

/**
 * A mirror to the [Metadata] annotation on a JVM class file, containing the metadata of Kotlin declarations declared in the class file.
 * Properties of this class correspond 1:1 to the properties of [Metadata].
 *
 * @param kind see [kind]
 * @param metadataVersion see [metadataVersion]
 * @param bytecodeVersion see [bytecodeVersion]
 * @param data1 see [data1]
 * @param data2 see [data2]
 * @param extraString see [extraString]
 * @param packageName see [packageName]
 * @param extraInt see [extraInt]
 */
class KotlinClassHeader(
    kind: Int?,
    metadataVersion: IntArray?,
    bytecodeVersion: IntArray?,
    data1: Array<String>?,
    data2: Array<String>?,
    extraString: String?,
    packageName: String?,
    extraInt: Int?
) {
    /**
     * A kind of the metadata this header encodes.
     *
     * @see Metadata.kind
     * @see CLASS_KIND
     * @see FILE_FACADE_KIND
     * @see SYNTHETIC_CLASS_KIND
     * @see MULTI_FILE_CLASS_FACADE_KIND
     * @see MULTI_FILE_CLASS_PART_KIND
     */
    val kind: Int = kind ?: 1

    /**
     * The version of the metadata provided in other properties of this header.
     *
     * @see Metadata.metadataVersion
     * @see COMPATIBLE_METADATA_VERSION
     */
    val metadataVersion: IntArray = metadataVersion ?: intArrayOf()

    /**
     * The version of the bytecode interface (naming conventions, signatures) of the corresponding class file.
     *
     * @see Metadata.bytecodeVersion
     * @see COMPATIBLE_BYTECODE_VERSION
     */
    val bytecodeVersion: IntArray = bytecodeVersion ?: intArrayOf()

    /**
     * The first array of strings used to encode the metadata.
     *
     * @see Metadata.data1
     */
    val data1: Array<String> = data1 ?: emptyArray()

    /**
     * The second array of strings used to encode the metadata.
     *
     * @see Metadata.data2
     */
    val data2: Array<String> = data2 ?: emptyArray()

    /**
     * An extra string field for the metadata.
     *
     * @see Metadata.extraString
     */
    val extraString: String = extraString ?: ""

    /**
     * Fully qualified name of the Kotlin package of the corresponding class, in case [JvmPackageName] was used.
     *
     * @see Metadata.packageName
     */
    val packageName: String = packageName ?: ""

    /**
     * An extra int field for the metadata.
     *
     * @see Metadata.extraInt
     */
    val extraInt: Int = extraInt ?: 0

    companion object {
        /**
         * A class file kind signifying that the corresponding class file contains a declaration of a Kotlin class.
         *
         * @see kind
         */
        const val CLASS_KIND = 1

        /**
         * A class file kind signifying that the corresponding class file is a compiled Kotlin file facade.
         *
         * @see kind
         */
        const val FILE_FACADE_KIND = 2

        /**
         * A class file kind signifying that the corresponding class file is synthetic, e.g. it's a class for lambda, `$DefaultImpls` class
         * for interface method implementations, `$WhenMappings` class for optimized `when` over enums, etc.
         *
         * @see kind
         */
        const val SYNTHETIC_CLASS_KIND = 3

        /**
         * A class file kind signifying that the corresponding class file is a compiled multi-file class facade.
         *
         * @see kind
         * @see JvmMultifileClass
         */
        const val MULTI_FILE_CLASS_FACADE_KIND = 4

        /**
         * A class file kind signifying that the corresponding class file is a compiled multi-file class part, i.e. an internal class
         * with method bodies and their metadata, accessed only from the corresponding facade.
         *
         * @see kind
         * @see JvmMultifileClass
         */
        const val MULTI_FILE_CLASS_PART_KIND = 5

        /**
         * The latest metadata version supported by this version of the library.
         *
         * @see metadataVersion
         */
        @JvmField
        val COMPATIBLE_METADATA_VERSION = JvmMetadataVersion.INSTANCE.toArray().copyOf()

        /**
         * The latest bytecode version supported by this version of the library.
         *
         * @see bytecodeVersion
         */
        @JvmField
        val COMPATIBLE_BYTECODE_VERSION = JvmBytecodeBinaryVersion.INSTANCE.toArray().copyOf()
    }
}
