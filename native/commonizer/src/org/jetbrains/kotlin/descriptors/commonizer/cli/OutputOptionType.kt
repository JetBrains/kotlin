/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cli

import java.io.File

internal object OutputOptionType : OptionType<File>("output-path", "Destination for commonized libraries") {
    override fun parse(rawValue: String, onError: (reason: String) -> Nothing): Option<File> {
        val file = File(rawValue)

        try {
            val valid = when {
                file.isDirectory -> file.listFiles()?.isEmpty() != false
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
