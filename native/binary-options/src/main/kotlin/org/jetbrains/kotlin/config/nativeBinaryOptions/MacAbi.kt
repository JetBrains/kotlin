/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.nativeBinaryOptions

/**
 * An architecture of macOS Catalyst.
 *
 * Overall, this class highlights the limits of [org.jetbrains.kotlin.konan.target.KonanTarget] class.
 * We would not need this binary option at all if we could directly pass target triple to the compiler.
 *
 * Could be replaced by [org.jetbrains.kotlin.konan.target.Architecture],
 * but it requires adding a dependency on another module.
 */
enum class MacAbi {
    X64,
    ARM64,
}