/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.jvm

/**
 * A mirror to the [Metadata] annotation on a JVM class file, containing the metadata of Kotlin declarations declared in the class file.
 * Properties of this class correspond to the properties of [Metadata], but the names are not shortened because there's no restriction
 * on the bytecode size here.
 *
 * Note that [Metadata] is not yet public (see https://youtrack.jetbrains.com/issue/KT-23602). In order to create an instance of
 * [KotlinClassHeader] from the [Metadata] annotation instance obtained reflectively, one could use Java code to workaround
 * the internal visibility error.
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
     * @see Metadata.k
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
     * @see Metadata.mv
     * @see COMPATIBLE_METADATA_VERSION
     */
    val metadataVersion: IntArray = metadataVersion ?: intArrayOf()

    /**
     * The version of the bytecode interface (naming conventions, signatures) of the corresponding class file.
     *
     * @see Metadata.bv
     * @see COMPATIBLE_BYTECODE_VERSION
     */
    val bytecodeVersion: IntArray = bytecodeVersion ?: intArrayOf()

    /**
     * The first array of strings used to encode the metadata.
     *
     * @see Metadata.d1
     */
    val data1: Array<String> = data1 ?: emptyArray()

    /**
     * The second array of strings used to encode the metadata.
     *
     * @see Metadata.d2
     */
    val data2: Array<String> = data2 ?: emptyArray()

    /**
     * An extra string field for the metadata.
     *
     * @see Metadata.xs
     */
    val extraString: String = extraString ?: ""

    /**
     * Fully qualified name of the Kotlin package of the corresponding class, in case [JvmPackageName] was used.
     *
     * @see Metadata.pn
     */
    val packageName: String = packageName ?: ""

    /**
     * An extra int field for the metadata.
     *
     * @see Metadata.xi
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
        val COMPATIBLE_METADATA_VERSION = intArrayOf(1, 1, 10)

        /**
         * The latest bytecode version supported by this version of the library.
         *
         * @see bytecodeVersion
         */
        @JvmField
        val COMPATIBLE_BYTECODE_VERSION = intArrayOf(1, 0, 2)
    }
}
