/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.internal

import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode

internal object WasmBinaryAttribute {
    val attribute: Attribute<String> = Attribute.of("org.jetbrains.kotlin.wasm.binary", String::class.java)
    val compilationNameAttribute: Attribute<String> = Attribute.of("org.jetbrains.kotlin.wasm.compilation.name", String::class.java)

    const val KLIB_ARTIFACT = "klib"
    const val KLIB_ATTRIBUTE_VALUE = "<$KLIB_ARTIFACT>"

    const val WASM_BINARY_DEVELOPMENT = "wasm-binary-development"
    const val WASM_BINARY_PRODUCTION = "wasm-binary-production"

    fun setupTransform(project: Project) {
        project.dependencies.artifactTypes.maybeCreate("jar").also { artifactType ->
            artifactType.attributes.attribute(attribute, WASM_BINARY_DEVELOPMENT)
        }

        project.dependencies.artifactTypes.maybeCreate(KLIB_ARTIFACT).also { artifactType ->
            artifactType.attributes.attribute(
                attribute,
                KLIB_ATTRIBUTE_VALUE
            )

            artifactType.attributes.attribute(
                compilationNameAttribute,
                KLIB_ATTRIBUTE_VALUE
            )
        }
    }

    fun modeToAttribute(mode: KotlinJsBinaryMode) = when (mode) {
        KotlinJsBinaryMode.DEVELOPMENT -> WASM_BINARY_DEVELOPMENT
        KotlinJsBinaryMode.PRODUCTION -> WASM_BINARY_PRODUCTION
    }
}