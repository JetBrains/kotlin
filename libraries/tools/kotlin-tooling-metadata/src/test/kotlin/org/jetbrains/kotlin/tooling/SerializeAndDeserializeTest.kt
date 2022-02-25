/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling

import org.jetbrains.kotlin.tooling.KotlinToolingMetadata.ProjectTargetMetadata
import kotlin.test.Test
import kotlin.test.assertEquals

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

class SerializeAndDeserializeTest {

    @Test
    fun sample1() = assertDeserializedMatchesOrigin(
        defaultKotlinToolingMetadata()
    )

    @Test
    fun sample2() = assertDeserializedMatchesOrigin(
        defaultKotlinToolingMetadata().copy(
            projectTargets = listOf(
                ProjectTargetMetadata(
                    target = "generic target",
                    platformType = "generic platform type",
                    extras = ProjectTargetMetadata.Extras()
                )
            )
        )
    )

    @Test
    fun sample3() = assertDeserializedMatchesOrigin(
        defaultKotlinToolingMetadata().copy(
            projectTargets = listOf(
                ProjectTargetMetadata(
                    target = "generic target",
                    platformType = "generic platform type",
                    extras = ProjectTargetMetadata.Extras(
                        jvm = ProjectTargetMetadata.JvmExtras(
                            jvmTarget = null,
                            withJavaEnabled = true
                        ),
                        android = ProjectTargetMetadata.AndroidExtras(
                            sourceCompatibility = "1.8",
                            targetCompatibility = "1.6"
                        ),
                        js = ProjectTargetMetadata.JsExtras(
                            isBrowserConfigured = true,
                            isNodejsConfigured = false
                        ),
                        native = ProjectTargetMetadata.NativeExtras(
                            konanTarget = "linuxX64",
                            konanVersion = "1.0-generic",
                            konanAbiVersion = "1.4.2"
                        )
                    )
                ),
                ProjectTargetMetadata(
                    target = "generic target 2 (with no extras)",
                    platformType = "generic platform type 2",
                    extras = ProjectTargetMetadata.Extras(
                        jvm = ProjectTargetMetadata.JvmExtras(
                            jvmTarget = "1.8",
                            withJavaEnabled = true
                        )
                    )
                )
            )
        )
    )
}

private fun assertDeserializedMatchesOrigin(origin: KotlinToolingMetadata) {
    val json = origin.toJsonString()
    val deserialized = KotlinToolingMetadata.parseJsonOrThrow(json)
    assertEquals(origin, deserialized)
}

private fun defaultKotlinToolingMetadata(): KotlinToolingMetadata {
    return KotlinToolingMetadata(
        schemaVersion = KotlinToolingMetadata.currentSchemaVersion,
        buildSystem = "generic build system",
        buildSystemVersion = "v1.0 (build system)",
        buildPlugin = "generic build plugin",
        buildPluginVersion = "v1.0 (build plugin)",
        projectSettings = KotlinToolingMetadata.ProjectSettings(
            isHmppEnabled = false,
            isCompatibilityMetadataVariantEnabled = true,
            isKPMEnabled = false,
        ),
        projectTargets = emptyList()
    )
}
