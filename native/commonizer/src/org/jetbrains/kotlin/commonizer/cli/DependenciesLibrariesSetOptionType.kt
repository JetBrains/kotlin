/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cli

import org.jetbrains.kotlin.commonizer.CommonizerDependency
import org.jetbrains.kotlin.commonizer.parseCommonizerDependency

internal abstract class DependenciesLibrariesSetOptionType(
    mandatory: Boolean,
    alias: String,
    description: String
) : OptionType<List<CommonizerDependency>>(
    mandatory = mandatory,
    alias = alias,
    description = description
) {
    override fun parse(rawValue: String, onError: (reason: String) -> Nothing): Option<List<CommonizerDependency>> {
        if (rawValue.isBlank()) {
            return Option(this, emptyList())
        }
        return Option(this, rawValue.split(";").map(::parseCommonizerDependency))
    }
}
