/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cli

import java.io.File

internal abstract class LibrariesSetOptionType(
    mandatory: Boolean,
    alias: String,
    description: String
) : OptionType<List<File>>(
    mandatory = mandatory,
    alias = alias,
    description = description
) {
    override fun parse(rawValue: String, onError: (reason: String) -> Nothing): Option<List<File>> {
        if (rawValue.isBlank()) {
            return Option(this, emptyList())
        }
        return Option(this, rawValue.split(";").map(::File))
    }
}
