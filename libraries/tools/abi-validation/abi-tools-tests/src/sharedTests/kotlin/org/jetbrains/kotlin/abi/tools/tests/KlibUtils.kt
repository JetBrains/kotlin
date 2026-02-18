/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.tests

import org.jetbrains.kotlin.abi.tools.AbiFilters
import org.jetbrains.kotlin.abi.tools.AbiTools
import org.jetbrains.kotlin.abi.tools.KlibDump
import org.jetbrains.kotlin.abi.tools.KlibTarget
import java.io.File
import java.io.FileNotFoundException

/**
 * Dumps a public ABI of a klib represented by [klibFile] using [filters] and merges it into this dump.
 *
 * To control which declarations are dumped, [filters] could be used. By default, no filters will be applied.
 *
 * It's an error to merge dumps having some targets in common.
 *
 * @throws IllegalArgumentException if this dump and [klibFile] shares same targets.
 * @throws IllegalStateException if a klib could not be loaded from [klibFile].
 * @throws FileNotFoundException if [klibFile] does not exist.
 */
internal fun KlibDump.mergeFromKlib(
    klibFile: File,
    filters: AbiFilters = AbiFilters.EMPTY
) {
    this.merge(AbiToolsImpl.extractKlibAbi(klibFile, KlibTarget("test"), filters))
}
