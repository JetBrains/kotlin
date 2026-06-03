/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.FileSerializer
import java.io.File
import java.io.Serializable as JavaSerializable

internal enum class GradleSwiftExportModuleType : JavaSerializable {
    SWIFT_ONLY, BRIDGES_TO_KOTLIN
}

@Serializable
internal data class GradleSwiftExportModules(
    val modules: List<GradleSwiftExportModule>,
    val timestamp: Long,
) : JavaSerializable

@Serializable
internal sealed class GradleSwiftExportModule : JavaSerializable {
    abstract val name: String
    abstract val type: GradleSwiftExportModuleType
    abstract val dependencies: List<String>

    @Serializable
    @SerialName("SWIFT_ONLY")
    data class SwiftOnly(
        @Serializable(with = FileSerializer::class) val swiftApi: File,
        override val name: String,
        override val dependencies: List<String>,
    ) : GradleSwiftExportModule() {
        override val type: GradleSwiftExportModuleType get() = GradleSwiftExportModuleType.SWIFT_ONLY
    }

    @Serializable
    @SerialName("BRIDGES_TO_KOTLIN")
    data class BridgesToKotlin(
        val files: GradleSwiftExportFiles,
        val bridgeName: String,
        override val name: String,
        override val dependencies: List<String>,
    ) : GradleSwiftExportModule() {
        override val type: GradleSwiftExportModuleType get() = GradleSwiftExportModuleType.BRIDGES_TO_KOTLIN
    }
}

@Serializable
internal data class GradleSwiftExportFiles(
    @Serializable(with = FileSerializer::class) val swiftApi: File,
    @Serializable(with = FileSerializer::class) val kotlinBridges: File,
    @Serializable(with = FileSerializer::class) val cHeaderBridges: File,
) : JavaSerializable
