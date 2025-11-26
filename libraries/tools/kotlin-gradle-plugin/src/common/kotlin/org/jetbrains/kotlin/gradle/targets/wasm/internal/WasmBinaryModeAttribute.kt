/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.internal

import org.gradle.api.attributes.Attribute
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode

internal object WasmBinaryModeAttribute {
    val attribute: Attribute<String> = Attribute.of("org.jetbrains.kotlin.wasm.binary.mode", String::class.java)

    const val PRODUCTION = "production"

    const val DEVELOPMENT = "development"

    fun KotlinJsBinaryMode.attributeByMode(): String = when (this) {
        KotlinJsBinaryMode.PRODUCTION -> PRODUCTION
        KotlinJsBinaryMode.DEVELOPMENT -> DEVELOPMENT
    }
}