/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.plugins.kotlin

enum class ComposeRuntimeVersion(val value: String) {
    v1_8("1.8"),
    v1_9("1.9"),
    ;

    fun supportsFeature(feature: ComposeRuntimeFeature): Boolean {
        return this >= feature.targetVersion
    }

    companion object {
        fun fromString(version: String): ComposeRuntimeVersion? {
            return entries.find { it.value == version } ?: error(
                "Unknown target runtime version: $version. " +
                        "Supported versions are: ${entries.joinToString { it.value }}"
            )
        }
    }
}

enum class ComposeRuntimeFeature(val targetVersion: ComposeRuntimeVersion) {
    SourceInfoParameterNames(ComposeRuntimeVersion.v1_9),
    ;
}

fun ComposeRuntimeVersion?.supportsFeature(
    feature: ComposeRuntimeFeature,
    detector: () -> Boolean
): Boolean = this?.supportsFeature(feature) ?: detector()