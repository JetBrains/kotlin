/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.jetbrains.kotlin.platform.wasm.WasmTarget

enum class KotlinWasmTargetType {
    WASI,
    JS;
}

fun KotlinWasmTargetType.toAttribute(): KotlinWasmTargetAttribute {
    return when(this) {
        KotlinWasmTargetType.WASI -> KotlinWasmTargetAttribute.wasi
        KotlinWasmTargetType.JS -> KotlinWasmTargetAttribute.js
    }
}

fun KotlinWasmTargetType.toCompilerTarget(): WasmTarget {
    return when(this) {
        KotlinWasmTargetType.WASI -> WasmTarget.WASI
        KotlinWasmTargetType.JS -> WasmTarget.JS
    }
}