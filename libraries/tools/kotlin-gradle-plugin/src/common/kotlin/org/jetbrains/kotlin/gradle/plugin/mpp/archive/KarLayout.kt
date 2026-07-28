/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.archive

import org.gradle.api.attributes.Attribute

internal object KarLayout {
    const val ARTIFACT_TYPE = "kar"
    const val XZ_ARTIFACT_TYPE = "xz"
    const val COMPRESSED_ARTIFACT_EXTENSION = "${ARTIFACT_TYPE}.${XZ_ARTIFACT_TYPE}"
    const val PACKING_DIRECTORY = "kar"

    const val CONFIGURATION_NAME = "kotlinArchive"

    const val METADATA_DIRECTORY_NAME = "metadata"
    const val TASK_NAME = "packKotlinArchive"


    object Attributes {
        enum class State {
            COMPRESSED,
            DECOMPRESSED,
            PLATFORM_ARTIFACTS_EXTRACTED,
            PSM_EXTRACTED,
            RESOURCES_EXTRACTED,
        }

        val state = Attribute.of("org.jetbrains.kotlin.kar.state", State::class.java)

        enum class CompressionMethod {
            XZ;
        }

        val compressionMethod = Attribute.of("org.jetbrains.kotlin.kar.compression.method", CompressionMethod::class.java)
    }
}
