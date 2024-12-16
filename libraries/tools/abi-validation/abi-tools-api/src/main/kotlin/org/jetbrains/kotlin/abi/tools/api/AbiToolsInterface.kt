/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.api

import org.jetbrains.kotlin.abi.tools.api.v2.AbiToolsV2
import java.io.File

/**
 * All features of Kotlin ABI Validation tool.
 *
 * @since 2.1.20
 */
public interface AbiToolsInterface {
    /**
     * A set of features for working with legacy format dumps, used in previous [Binary Compatibility Validator plugin](https://github.com/Kotlin/binary-compatibility-validator).
     */
    public val v2: AbiToolsV2

    /**
     * Compare two files line-by-line.
     *
     * @return `null` if there are no differences, diff string otherwise.
     *
     * @throws java.io.FileNotFoundException if [expectedFile] and/or [actualFile] does not exist.
     */
    public fun filesDiff(expectedFile: File, actualFile: File): String?
}