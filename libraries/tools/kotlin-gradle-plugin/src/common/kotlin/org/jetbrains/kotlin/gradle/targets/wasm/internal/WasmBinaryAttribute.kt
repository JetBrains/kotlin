/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.internal

import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.jetbrains.kotlin.gradle.utils.registerTransformForArtifactType

internal object WasmBinaryAttribute {
    val attribute: Attribute<String> = Attribute.of("org.jetbrains.kotlin.wasm.binary", String::class.java)

    const val KLIB = "klib"

    const val WASM_BINARY = "wasm-binary"

    fun setupTransform(project: Project) {
        project.dependencies.artifactTypes.maybeCreate(WASM_BINARY).also { artifactType ->
            artifactType.attributes.attribute(WasmBinaryAttribute.attribute, WASM_BINARY)
        }

        project.dependencies.artifactTypes.maybeCreate(WasmBinaryAttribute.KLIB).also { artifactType ->
            artifactType.attributes.attribute(
                WasmBinaryAttribute.attribute,
                WasmBinaryAttribute.KLIB
            )
        }
    }
}