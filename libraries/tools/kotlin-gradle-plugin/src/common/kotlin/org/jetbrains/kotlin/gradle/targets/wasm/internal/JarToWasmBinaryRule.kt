/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.internal

import org.gradle.api.attributes.AttributeCompatibilityRule
import org.gradle.api.attributes.CompatibilityCheckDetails

class JarToWasmBinaryRule : AttributeCompatibilityRule<String> {
    // Implement the execute method which will check compatibility
    override fun execute(details: CompatibilityCheckDetails<String>) {
        // Switch case to check the consumer value for supported Java versions

        if (details.producerValue == "jar" && details.consumerValue == WasmBinaryTransform.ARTIFACT_TYPE) {
            details.compatible()
        }
    }
}

class KlibToWasmBinaryCInteropRule : AttributeCompatibilityRule<String> {
    // Implement the execute method which will check compatibility
    override fun execute(details: CompatibilityCheckDetails<String>) {
        // Switch case to check the consumer value for supported Java versions

        if (details.producerValue == "klib" && details.consumerValue == WasmBinaryTransform.ARTIFACT_TYPE) {
            details.compatible()
        }
    }
}