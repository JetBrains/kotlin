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
 * @since 2.2.0
 */
public interface AbiToolsV2 {
    /**
     * Print ABI dump for JVM from [inputFiles] into specified [appendable].
     * It is possible to pass class-files or jar files in [inputFiles].
     *
     * To control which declarations are passed to the dump, [filters] could be used. By default, no filters will be applied.
     *
     * A class declaration with internal visibility will be printed as public if [internalDeclarationsAsPublic] returns `true` for its name.
     */
    public fun <T : Appendable> printJvmDump(
        appendable: T,
        inputFiles: Iterable<File>,
        filters: AbiFilters
    )

    /**
     * Create an empty KLib dump without any declarations and targets.
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
     * Get an ABI from a zipped or unpacked KLib specified in [klib].
     *
     * Original target will be overridden by [target] if it's not `null`.
     *
     * To control which declarations are passed to the dump, [filters] could be used. By default, no filters will be applied.
     *
     * @throws IllegalStateException if a KLib could not be loaded from [klib].
     * @throws java.io.FileNotFoundException if file or directory [klib] does not exist.
     */
    public fun extractKlibAbi(klib: File, target: KlibTarget? = null, filters: AbiFilters = AbiFilters.EMPTY): KlibDump
}