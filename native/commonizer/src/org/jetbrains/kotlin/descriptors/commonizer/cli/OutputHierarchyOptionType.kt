/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cli

import org.jetbrains.kotlin.commonizer.api.SharedCommonizerTarget
import org.jetbrains.kotlin.commonizer.api.parseCommonizerTarget

internal object OutputHierarchyOptionType : OptionType<SharedCommonizerTarget>(
    alias = "output-hierarchy",
    description = "Shared commonizer target representing the commonized output hierarchy",
    mandatory = true
) {
    override fun parse(rawValue: String, onError: (reason: String) -> Nothing): Option<SharedCommonizerTarget> {
        return try {
            Option(this, parseCommonizerTarget(rawValue) as SharedCommonizerTarget)
        } catch (t: Throwable) {
            onError("Failed parsing output-hierarchy ($rawValue): ${t.message}")
        }
    }
}
