/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.ib

import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

internal data class InteropBundle(val root: File) {
    fun resolve(target: KonanTarget, fileName: String): File = root.resolve(target.name).resolve(fileName)
    fun listLibraries(target: KonanTarget): Set<File> = root.resolve(target.name).listFiles().orEmpty().filter(::isLibrary).toSet()
    fun listLibraries(): Set<File> = root.walkTopDown().maxDepth(2).filter(::isLibrary).toSet()
}

private fun isLibrary(file: File): Boolean = file.exists() && file.extension == "klib"
