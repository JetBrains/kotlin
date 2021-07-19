/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.gradle.utils.appendLine

internal data class CInteropCommonizerGroup(
    @get:Input val targets: Set<SharedCommonizerTarget>,
    @get:Input val interops: Set<CInteropIdentifier>
) {
    @Suppress("deprecation")
    override fun toString(): String {
        return buildString {
            appendln("InteropsGroup {")
            appendln("targets: ")
            targets.sortedBy { it.targets.size }.forEach { target ->
                appendln("    $target")
            }
            appendln()
            appendln("interops: ")
            interops.sortedBy { it.toString() }.forEach { interop ->
                appendln("    $interop")
            }
            appendLine("}")
        }
    }
}