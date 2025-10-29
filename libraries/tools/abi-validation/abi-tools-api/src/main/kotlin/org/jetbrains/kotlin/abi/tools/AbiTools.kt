/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools

import java.io.File
import java.util.ServiceLoader.load

/**
 * The set of features for working with ABI dumps.
 *
 * @since 2.3.20
 */
public interface AbiTools {
    /**
     * Prints an ABI dump for JVM from [inputFiles] into the specified [appendable].
     * It is possible to pass class-files or jar files in [inputFiles].
     *
     * To control which declarations are passed to the dump, [filters] could be used. By default, no filters will be applied.
     */
    public fun <T : Appendable> printJvmDump(appendable: T, inputFiles: Iterable<File>, filters: AbiFilters)

    /**
     * Creates an empty KLib dump without any declarations and targets.
     */
    public fun createKlibDump(): KlibDump

    /**
     * Loads a KLib dump from a dump file.
     *
     * @throws IllegalArgumentException if [dumpFile] is empty.
     * @throws IllegalArgumentException if [dumpFile] is not a file.
     * @throws java.io.FileNotFoundException if [dumpFile] does not exist.
     */
    public fun loadKlibDump(dumpFile: File): KlibDump

    /**
     * Reads a KLib dump from a textual form.
     *
     * @throws IllegalArgumentException if this dump and the provided [dump] shares same targets.
     * @throws IllegalArgumentException if the provided [dump] is empty.
     */
    public fun loadKlibDump(dump: CharSequence): KlibDump

    /**
     * Gets an ABI from a zipped or an unpacked KLib specified in the [klib].
     *
     * Original target will be overridden by the [target] if it's not `null`.
     *
     * To control which declarations are passed to the dump, [filters] could be used. By default, no filters will be applied.
     *
     * @throws IllegalStateException if a KLib could not be loaded from the [klib].
     * @throws java.io.FileNotFoundException if file or directory [klib] does not exist.
     */
    public fun extractKlibAbi(klib: File, target: KlibTarget? = null, filters: AbiFilters = AbiFilters.EMPTY): KlibDump

    /**
     * Compares two files line-by-line.
     *
     * @return `null` if there are no differences, diff string otherwise.
     *
     * @throws java.io.FileNotFoundException if the [expectedFile] and/or the [actualFile] does not exist.
     */
    public fun filesDiff(expectedFile: File, actualFile: File): String?

    public companion object {
        /**
         * Gets an implementation of [AbiTools] by [java.util.ServiceLoader].
         *
         * Searching of implementation takes place in the context of the specified [classLoader].
         * If [classLoader] is not specified or null then class loader of [AbiTools] class will be used.
         *
         * There is no guarantee that for the same [classLoader] only the same implementation of [AbiTools] will be returned.
         *
         * @throws IllegalStateException if no implementation found.
         * @throws IllegalStateException if there are more than one implementation found.
         */
        public fun getInstance(classLoader: ClassLoader? = null): AbiTools {
            val implementations = load(AbiToolsFactory::class.java, classLoader ?: this::class.java.classLoader)
            implementations.firstOrNull() ?: error("The classpath contains no implementation for ${AbiTools::class.qualifiedName}")
            val factory = implementations.singleOrNull()
                ?: error("The classpath contains more than one implementation for ${AbiTools::class.qualifiedName}")
            return factory.get()
        }
    }
}