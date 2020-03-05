/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cli

import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget

internal object NativeTargetsOptionType : OptionType<List<KonanTarget>>("targets", "Comma-separated list of hardware targets") {
    private val hostManager = HostManager()

    override fun parse(rawValue: String, onError: (reason: String) -> Nothing): Option<List<KonanTarget>> {
        val targetNames = rawValue.split(',')
        if (targetNames.isEmpty()) onError("No hardware targets specified: $rawValue")

        val targets = targetNames.mapTo(HashSet<KonanTarget>()) { targetName ->
            hostManager.targets[targetName] ?: onError("Unknown hardware target: $targetName")
        }.toList()

        return Option(this, targets)
    }
}
