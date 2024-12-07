/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.api.Named
import org.gradle.api.attributes.*
import java.io.Serializable

// For Gradle attributes
@Suppress("EnumEntryName")
enum class KotlinWasmTargetAttribute : Named, Serializable {
    wasi,
    js;

    override fun getName(): String =
        name

    override fun toString(): String =
        getName()

    companion object {
        val wasmTargetAttribute = Attribute.of(
            "org.jetbrains.kotlin.wasm.target",
            KotlinWasmTargetAttribute::class.java
        )

        fun setupAttributesMatchingStrategy(attributesSchema: AttributesSchema) {
            attributesSchema.attribute(wasmTargetAttribute)
        }
    }
}