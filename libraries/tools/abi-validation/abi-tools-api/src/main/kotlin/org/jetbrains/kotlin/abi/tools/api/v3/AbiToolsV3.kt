/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.api.v3

import org.jetbrains.kotlin.abi.tools.api.AbiFilters
import java.io.File

/**
 * A set of features for working with ABI dumps in version 3 format.
 *
 * @since 2.1.20
 */
public interface AbiToolsV3 {
    /**
     * Create empty ABI dump without any declarations and targets.
     */
    public fun createDump(): AbiDump

    /**
     * Get an ABI from a klib file for specified [target].
     *
     * To control which declarations are passed to the dump, [filters] could be used. By default, no filters will be applied.
     *
     * @throws IllegalStateException if a klib could not be loaded from [klibFile].
     * @throws java.io.FileNotFoundException if [klibFile] does not exist.
     */
    public fun extractKlibAbi(klibFile: File, target: DumpTarget, filters: AbiFilters): AbiDump

    /**
     * Get an ABI for JVM class-files for specified [target].
     *
     * To control which declarations are passed to the dump, [filters] could be used. By default, no filters will be applied.
     */
    public fun extractJvmAbi(classfiles: Sequence<File>, target: DumpTarget, filters: AbiFilters): AbiDump

    /**
     * Reads an ABI dump from a file.
     *
     * @throws IllegalArgumentException if [dumpFile] is empty.
     * @throws IllegalArgumentException if [dumpFile] is not a file.
     * @throws java.io.FileNotFoundException if [dumpFile] does not exist.
     */
    public fun loadDump(dumpFile: File): AbiDump

    /**
     * Reads an ABI dump from text lines.
     */
    public fun loadDump(dump: Iterable<String>): AbiDump

    /**
     * Reads an ABI dump from a textual form.
     */
    public fun loadDump(dump: CharSequence): AbiDump
}