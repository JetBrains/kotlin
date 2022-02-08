/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cli

import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.KonanTarget.Companion.predefinedTargets

internal object NativeTargetsOptionType : OptionType<List<KonanTarget>>(
    NATIVE_TARGETS_ALIAS, "Comma-separated list of hardware targets", mandatory = false
) {
    override fun parse(rawValue: String, onError: (reason: String) -> Nothing): Option<List<KonanTarget>> {
        val targetNames = rawValue.split(',')
        if (targetNames.isEmpty()) onError("No hardware targets specified: $rawValue")

        val targets = targetNames.mapTo(HashSet()) { targetName ->
            predefinedTargets[targetName] ?: onError("Unknown hardware target: $targetName")
        }.sortedBy { it.name }

        return Option(this, targets)
    }
}
