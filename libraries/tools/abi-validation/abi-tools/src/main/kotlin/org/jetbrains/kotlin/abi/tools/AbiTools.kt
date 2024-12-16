/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools

import org.jetbrains.kotlin.abi.tools.api.AbiToolsInterface
import org.jetbrains.kotlin.abi.tools.api.v2.AbiToolsV2
import java.io.File

internal object AbiTools : AbiToolsInterface {
    override val v2: AbiToolsV2
        get() = TODO("Not implemented yet")

    override fun filesDiff(expectedFile: File, actualFile: File): String? {
        TODO("Not yet implemented")
    }
}