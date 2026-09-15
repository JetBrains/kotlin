/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.component.internal

import org.gradle.api.Project
import org.gradle.api.attributes.Attribute

internal object WitExtractBinaryAttribute {
    val attribute: Attribute<String> = Attribute.of("org.jetbrains.kotlin.wasm.wit", String::class.java)

    const val KLIB_ARTIFACT = "klib"
    const val KLIB_ATTRIBUTE_VALUE = "<$KLIB_ARTIFACT>"

    const val WASM_EXTRACTED_WIT = "extracted-wit"

    fun setupTransform(project: Project) {
        project.dependencies.artifactTypes.maybeCreate("jar").also { artifactType ->
            artifactType.attributes.attribute(attribute, KLIB_ARTIFACT)
        }

        project.dependencies.artifactTypes.maybeCreate(KLIB_ARTIFACT).also { artifactType ->
            artifactType.attributes.attribute(
                attribute,
                KLIB_ATTRIBUTE_VALUE
            )
        }
    }
}
