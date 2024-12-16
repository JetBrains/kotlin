/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.api.v2

import org.jetbrains.kotlin.abi.tools.api.AbiFilters
import java.io.File

/**
 * A set of features for working with legacy format dumps,
 * used in previous [Binary Compatibility Validator plugin](https://github.com/Kotlin/binary-compatibility-validator).
 *
 * @since 2.1.20
 */
public interface AbiToolsV2 {
    /**
     * Print ABI dump for JVM class-files into specified [appendable].
     */
    public fun <T : Appendable> printJvmDump(appendable: T, classfiles: Iterable<File>, filters: AbiFilters)

    /**
     * Create empty KLib dump without any declarations and targets.
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
     * Get an ABI from a KLib file with specified [target].
     *
     * To control which declarations are passed to the dump, [filters] could be used. By default, no filters will be applied.
     *
     * @throws IllegalStateException if a KLib could not be loaded from [klibFile].
     * @throws java.io.FileNotFoundException if [klibFile] does not exist.
     */
    public fun extractKlibAbi(klibFile: File, target: KlibTarget, filters: AbiFilters = AbiFilters.Companion.EMPTY): KlibDump
}