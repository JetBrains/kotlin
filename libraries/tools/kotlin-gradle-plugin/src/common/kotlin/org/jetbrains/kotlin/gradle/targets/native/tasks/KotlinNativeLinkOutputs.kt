/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks

import org.jetbrains.kotlin.gradle.tasks.asValidFrameworkName
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

/**
 * Files the compiler writes into [destinationDirectory] for one link call: the binary,
 * the C header, the `.def` file and the dSYM. See `OutputFiles.kt` and `Linker.kt` in
 * the compiler for where each of them comes from.
 */
internal class KotlinNativeLinkOutputs(
    destinationDirectory: File,
    outputKind: CompilerOutputKind,
    private val target: KonanTarget,
    baseName: String,
    debuggable: Boolean,
    isStaticFramework: Boolean,
    freeCompilerArgs: List<String>,
) {
    private val isFramework = outputKind == CompilerOutputKind.FRAMEWORK

    private val isLibrary = outputKind == CompilerOutputKind.STATIC || outputKind == CompilerOutputKind.DYNAMIC

    // Static archives go through libtool, which produces no dSYM
    private val isStaticArchive = outputKind == CompilerOutputKind.STATIC || isStaticFramework

    // Light debug info is on by default for Apple targets, so release binaries get a dSYM too
    private val lightDebug = "-Xadd-light-debug=disable" !in freeCompilerArgs

    // Headers only, the compiler never links
    private val omitFrameworkBinary = isFramework && "-Xomit-framework-binary" in freeCompilerArgs

    private val producesDsym = target.family.isAppleFamily &&
            !isStaticArchive &&
            !omitFrameworkBinary &&
            (debuggable || lightDebug)

    /** Main output path without the suffix, e.g. `.../libfoo` for `.../libfoo.so`. */
    private val outputName: File = destinationDirectory.resolve(
        "${outputKind.prefix(target)}$baseName".let {
            if (isFramework || isLibrary) it.asValidFrameworkName else it
        }
    )

    /** The binary. For frameworks this is a directory. */
    val mainOutput: File = File(outputName.path + outputKind.suffix(target))

    // listOfNotNull rather than buildList: Gradle's configuration cache can't serialize ListBuilder
    val files: List<File> = listOfNotNull(
        mainOutput.takeUnless { isFramework },
        File("${outputName.path}_api.h").takeIf { isLibrary },
        File("${outputName.path}.def").takeIf { isLibrary && target.family == Family.MINGW },
    )

    val directories: List<File> = listOfNotNull(
        mainOutput.takeIf { isFramework },
        File("${mainOutput.path}.dSYM").takeIf { producesDsym },
    )
}
