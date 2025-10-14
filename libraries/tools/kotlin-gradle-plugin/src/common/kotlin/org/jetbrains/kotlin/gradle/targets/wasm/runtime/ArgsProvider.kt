/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.runtime

import java.nio.file.Path

internal fun interface ArgsProvider {
    fun provideArgs(workingDir: Path, wasmFile: Path): List<String>
}