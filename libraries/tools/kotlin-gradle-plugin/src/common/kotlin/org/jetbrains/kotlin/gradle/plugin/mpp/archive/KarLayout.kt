/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.archive

import org.gradle.api.attributes.Attribute
import org.jetbrains.kotlin.gradle.plugin.mpp.MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME

internal object KarLayout {
    const val XZ_ARTIFACT_TYPE = "xz"
    const val KAR_EXTENSION = "kar"
    const val KAR_XZ_PACKED_EXTENSION = "${KAR_EXTENSION}.${XZ_ARTIFACT_TYPE}"
    const val PACKING_DIRECTORY = "kar"
    const val ASSEMBLE_DIRECTORY = "kar/assemble"

    const val METADATA_DIRECTORY_NAME = "metadata"
    const val PLATFORM_KLIBS_DIRECTORY_NAME = "platform"
    const val CINTEROP_KLIBS_DIRECTORY_NAME = "cinterop"
    const val RESOURCES_DIRECTORY_NAME = "resources"

    const val PSM_FILE_NAME = MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME
    const val PSM_FILE_PATH = "$METADATA_DIRECTORY_NAME/$PSM_FILE_NAME"
    const val MANIFEST_FILE_PATH = "manifest.json"

    const val PACK_TASK_NAME = "packKotlinArchive"
    const val ASSEMBLE_TASK_NAME = "assembleKotlinArchive"

    object Attributes {
        object State {
            const val COMPRESSED = "compressed"
            const val DECOMPRESSED = "decompressed"
            const val PLATFORM_ARTIFACTS_EXTRACTED = "platform-artifacts-extracted"
            const val PSM_EXTRACTED = "psm-extracted"
            const val RESOURCES_EXTRACTED = "resources-extracted"
        }

        val state: Attribute<String> = Attribute.of("org.jetbrains.kotlin.kar.state", String::class.java)

        object CompressionMethod {
            const val UNKNOWN = "unknown"
            const val NONE = "none"
            const val XZ = "xz"
        }

        val compressionMethod: Attribute<String> = Attribute.of("org.jetbrains.kotlin.kar.compression.method", String::class.java)
    }
}
