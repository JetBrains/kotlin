/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal

import java.io.File
import java.io.Serializable

internal enum class GradleSwiftExportModuleType : Serializable {
    SWIFT_ONLY, BRIDGES_TO_KOTLIN
}

internal sealed class GradleSwiftExportModule(
    open val name: String,
    val type: GradleSwiftExportModuleType,
    open val dependencies: List<String>,
) : Serializable {

    data class SwiftOnly(
        val swiftApi: File,
        override val name: String,
        override val dependencies: List<String>,
    ) : GradleSwiftExportModule(name, GradleSwiftExportModuleType.SWIFT_ONLY, dependencies)

    data class BridgesToKotlin(
        val files: GradleSwiftExportFiles,
        val bridgeName: String,
        override val name: String,
        override val dependencies: List<String>,
    ) : GradleSwiftExportModule(name, GradleSwiftExportModuleType.BRIDGES_TO_KOTLIN, dependencies)
}

internal data class GradleSwiftExportFiles(
    val swiftApi: File,
    val kotlinBridges: File,
    val cHeaderBridges: File,
) : Serializable