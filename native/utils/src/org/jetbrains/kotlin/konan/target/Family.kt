/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

enum class Family(
    val exeSuffix: String,
    val dynamicPrefix: String,
    val dynamicSuffix: String,
    val staticPrefix: String,
    val staticSuffix: String
) {
    OSX("kexe", "lib", "dylib", "lib", "a"),
    IOS("kexe", "lib", "dylib", "lib", "a"),
    TVOS("kexe", "lib", "dylib", "lib", "a"),
    WATCHOS("kexe", "lib", "dylib", "lib", "a"),
    LINUX("kexe", "lib", "so", "lib", "a"),
    MINGW("exe", "", "dll", "lib", "a"),
    ANDROID("kexe", "lib", "so", "lib", "a"),
    WASM("wasm", "", "wasm", "", "wasm"),
    ZEPHYR("o", "lib", "a", "lib", "a");

    val isAppleFamily: Boolean
        get() = this == OSX || this == IOS || this == TVOS || this == WATCHOS
}
