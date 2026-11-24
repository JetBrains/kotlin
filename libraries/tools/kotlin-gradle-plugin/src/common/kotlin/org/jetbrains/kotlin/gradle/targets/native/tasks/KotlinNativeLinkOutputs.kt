/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks

import org.jetbrains.kotlin.gradle.tasks.asValidFrameworkName
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

/**
 * What the compiler writes into [destinationDirectory] for one link call.
 * Naming follows the compiler's `OutputFiles.kt`.
 */
internal class KotlinNativeLinkOutputs(
    destinationDirectory: File,
    outputKind: CompilerOutputKind,
    private val target: KonanTarget,
    baseName: String,
) {
    private val isFramework = outputKind == CompilerOutputKind.FRAMEWORK

    private val isLibrary = outputKind == CompilerOutputKind.STATIC || outputKind == CompilerOutputKind.DYNAMIC

    /** Main output path without the suffix, e.g. `.../libfoo` for `.../libfoo.so`. */
    private val outputName: File = destinationDirectory.resolve(
        "${outputKind.prefix(target)}$baseName".let {
            if (isFramework || isLibrary) it.asValidFrameworkName else it
        }
    )

    /** The binary itself; a directory for frameworks. */
    val mainOutput: File = File(outputName.path + outputKind.suffix(target))
}
