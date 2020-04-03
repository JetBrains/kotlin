/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cli

import java.io.File

internal object NativeDistributionOptionType : OptionType<File>("distribution-path", "Path to the Kotlin/Native distribution") {
    override fun parse(rawValue: String, onError: (reason: String) -> Nothing): Option<File> {
        val file = File(rawValue)

        try {
            if (!file.isDirectory) onError("Kotlin/Native distribution directory does not exist: $rawValue")
        } catch (_: Exception) {
            onError("Access failure to the Kotlin/Native distribution directory: $rawValue")
        }

        return Option(this, file)
    }
}
