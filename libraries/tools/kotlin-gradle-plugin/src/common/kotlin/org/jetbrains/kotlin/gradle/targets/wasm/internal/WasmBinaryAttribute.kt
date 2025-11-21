/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.internal

import org.gradle.api.Project
import org.gradle.api.attributes.Attribute

internal object WasmBinaryAttribute {
    val attribute: Attribute<String> = Attribute.of("org.jetbrains.kotlin.wasm.binary", String::class.java)

    const val KLIB = "klib"

    const val WASM_BINARY = "wasm-binary"

    fun setupTransform(project: Project) {
        project.dependencies.artifactTypes.maybeCreate("jar").also { artifactType ->
            artifactType.attributes.attribute(attribute, WASM_BINARY)
        }

        project.dependencies.artifactTypes.maybeCreate(KLIB).also { artifactType ->
            artifactType.attributes.attribute(
                attribute,
                KLIB
            )
        }
    }
}