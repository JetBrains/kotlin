/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cli

import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.commonizer.parseCommonizerTarget

internal object OutputCommonizerTargetsOptionType : OptionType<Set<SharedCommonizerTarget>>(
    alias = OUTPUT_COMMONIZER_TARGETS_ALIAS,
    description = "All output targets separated with ';'",
    mandatory = true
) {
    override fun parse(rawValue: String, onError: (reason: String) -> Nothing): Option<Set<SharedCommonizerTarget>> {
        return try {
            Option(
                this, rawValue.split(";")
                    .map { it.trim() }.filter { it.isNotEmpty() }
                    .map(::parseCommonizerTarget)
                    .map { it as SharedCommonizerTarget }.toSet()
            )
        } catch (t: Throwable) {
            onError("Failed parsing output-targets ($rawValue): ${t.message}")
        }
    }
}
