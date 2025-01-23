/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.nodejs

import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator

/**
 * An object that implements the `HasPlatformDisambiguator` interface to provide information specific to the Wasm platform.
 *
 * This object is used to identify the Wasm Kotlin platform within the context of platform-specific operations, such as
 * task naming and extension disambiguation.
 */
internal object WasmPlatformDisambiguator : HasPlatformDisambiguator {
    override val platformDisambiguator: String
        get() = wasmPlatform

    private val wasmPlatform: String
        get() = KotlinPlatformType.wasm.name
}