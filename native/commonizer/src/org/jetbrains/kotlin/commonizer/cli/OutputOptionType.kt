/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cli

import java.io.File

internal object OutputOptionType : OptionType<File>(OUTPUT_PATH_ALIAS, "Destination for commonized libraries") {
    override fun parse(rawValue: String, onError: (reason: String) -> Nothing): Option<File> {
        val file = File(rawValue)

        try {
            val valid = when {
                file.isDirectory -> true
                file.exists() -> false
                else -> {
                    if (!file.mkdirs()) onError("Destination can't be created: $rawValue")
                    true
                }
            }

            if (!valid) onError("Destination is not empty: $rawValue")
        } catch (_: Exception) {
            onError("Access failure to the destination directory: $rawValue")
        }

        return Option(this, file)
    }
}
