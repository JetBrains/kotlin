/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling

import org.intellij.lang.annotations.Language
import org.junit.Test
import kotlin.test.*

class DeserializeStringTest {
    @Test
    fun `schemaVersion 1 0 0`() {
        @Language("JSON") val json = """
        {
            "schemaVersion": "1.0.0",
            "buildSystem": "Gradle",
            "buildSystemVersion": "6.7",
            "buildPlugin": "org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper",
            "buildPluginVersion": "2.0.255-SNAPSHOT",
            "projectSettings": {
              "isHmppEnabled": false,
              "isCompatibilityMetadataVariantEnabled": true
            },
            "projectTargets": [
              {
                "target": "org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget",
                "platformType": "androidJvm",
                "extras": {
                  "android": {
                    "sourceCompatibility": "1.7",
                    "targetCompatibility": "1.7"
                  }
                }
              },
              {
                "target": "org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget",
                "platformType": "js",
                "extras": {
                  "js": {
                    "isBrowserConfigured": true,
                    "isNodejsConfigured": true
                  }
                }
              },
              {
                "target": "org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget",
                "platformType": "jvm",
                "extras": {
                  "jvm": {
                    "jvmTarget": "1.8",
                    "withJavaEnabled": false
                  }
                }
              },
              {
                "target": "org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests",
                "platformType": "native",
                "extras": {
                  "native": {
                    "konanTarget": "linux_x64",
                    "konanVersion": "1.5-dev-17775",
                    "konanAbiVersion": "1.4.2"
                  }
                }
              },
              {
                "target": "org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget",
                "platformType": "common"
              }
            ]
        }
            """.trimIndent()

        val metadata = KotlinToolingMetadata.parseJsonOrThrow(json)
        assertEquals("1.0.0", metadata.schemaVersion)
        assertEquals("Gradle", metadata.buildSystem)
        assertEquals("6.7", metadata.buildSystemVersion)
        assertEquals("org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper", metadata.buildPlugin)
        assertEquals("2.0.255-SNAPSHOT", metadata.buildPluginVersion)
        assertFalse(metadata.projectSettings.isHmppEnabled)
        assertTrue(metadata.projectSettings.isCompatibilityMetadataVariantEnabled)
        assertFalse(metadata.projectSettings.isKPMEnabled)
        assertEquals(5, metadata.projectTargets.size, "Expected exactly 4 targets")

        val androidJvmTarget = metadata.projectTargets.single { it.platformType == "androidJvm" }
        assertEquals("org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget", androidJvmTarget.target)
        assertEquals("1.7", androidJvmTarget.extras.android?.sourceCompatibility)
        assertEquals("1.7", androidJvmTarget.extras.android?.targetCompatibility)
        assertNull(androidJvmTarget.extras.jvm)
        assertNull(androidJvmTarget.extras.js)
        assertNull(androidJvmTarget.extras.native)

        val jsTarget = metadata.projectTargets.single { it.platformType == "js" }
        assertEquals("org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget", jsTarget.target)
        assertEquals(true, jsTarget.extras.js?.isBrowserConfigured)
        assertEquals(true, jsTarget.extras.js?.isNodejsConfigured)
        assertNull(jsTarget.extras.android)
        assertNull(jsTarget.extras.jvm)
        assertNull(jsTarget.extras.native)

        val jvmTarget = metadata.projectTargets.single { it.platformType == "jvm" }
        assertEquals("org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget", jvmTarget.target)
        assertEquals(false, jvmTarget.extras.jvm?.withJavaEnabled)
        assertEquals("1.8", jvmTarget.extras.jvm?.jvmTarget)
        assertNull(jvmTarget.extras.android)
        assertNull(jvmTarget.extras.js)
        assertNull(jvmTarget.extras.native)

        val nativeTarget = metadata.projectTargets.single { it.platformType == "native" }
        assertEquals("org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests", nativeTarget.target)
        val nativeExtras = assertNotNull(nativeTarget.extras.native)
        assertEquals("linux_x64", nativeExtras.konanTarget)
        assertEquals("1.5-dev-17775", nativeExtras.konanVersion)
        assertEquals("1.4.2", nativeExtras.konanAbiVersion)
        assertNull(nativeTarget.extras.android)
        assertNull(nativeTarget.extras.jvm)
        assertNull(nativeTarget.extras.js)

        val commonTarget = metadata.projectTargets.single { it.platformType == "common" }
        assertEquals("org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget", commonTarget.target)
        assertNull(commonTarget.extras.android)
        assertNull(commonTarget.extras.jvm)
        assertNull(commonTarget.extras.js)
        assertNull(commonTarget.extras.native)
    }

    @Test
    fun `schemaVersion 1 1 0`() {
        @Language("JSON") val json = """
        {
            "schemaVersion": "1.1.0",
            "buildSystem": "Gradle",
            "buildSystemVersion": "7.1",
            "buildPlugin": "org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper",
            "buildPluginVersion": "2.0.255-SNAPSHOT",
            "projectSettings": {
              "isHmppEnabled": false,
              "isCompatibilityMetadataVariantEnabled": true,
              "isKPMEnabled": true
            },
            "projectTargets": [
              {
                "target": "org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget",
                "platformType": "androidJvm",
                "extras": {
                  "android": {
                    "sourceCompatibility": "1.7",
                    "targetCompatibility": "1.7"
                  }
                }
              },
              {
                "target": "org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget",
                "platformType": "js",
                "extras": {
                  "js": {
                    "isBrowserConfigured": true,
                    "isNodejsConfigured": true
                  }
                }
              },
              {
                "target": "org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget",
                "platformType": "jvm",
                "extras": {
                  "jvm": {
                    "jvmTarget": "1.8",
                    "withJavaEnabled": false
                  }
                }
              },
              {
                "target": "org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests",
                "platformType": "native",
                "extras": {
                  "native": {
                    "konanTarget": "linux_x64",
                    "konanVersion": "1.5-dev-17775",
                    "konanAbiVersion": "1.4.2"
                  }
                }
              }
            ]
        }
            """.trimIndent()

        val metadata = KotlinToolingMetadata.parseJsonOrThrow(json)
        assertEquals("1.1.0", metadata.schemaVersion)
        assertEquals("Gradle", metadata.buildSystem)
        assertEquals("7.1", metadata.buildSystemVersion)
        assertEquals("org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper", metadata.buildPlugin)
        assertEquals("2.0.255-SNAPSHOT", metadata.buildPluginVersion)
        assertFalse(metadata.projectSettings.isHmppEnabled)
        assertTrue(metadata.projectSettings.isCompatibilityMetadataVariantEnabled)
        assertTrue(metadata.projectSettings.isKPMEnabled)
        assertEquals(4, metadata.projectTargets.size, "Expected exactly 4 targets")

        val androidJvmTarget = metadata.projectTargets.single { it.platformType == "androidJvm" }
        assertEquals("org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget", androidJvmTarget.target)
        assertEquals("1.7", androidJvmTarget.extras.android?.sourceCompatibility)
        assertEquals("1.7", androidJvmTarget.extras.android?.targetCompatibility)
        assertNull(androidJvmTarget.extras.jvm)
        assertNull(androidJvmTarget.extras.js)
        assertNull(androidJvmTarget.extras.native)

        val jsTarget = metadata.projectTargets.single { it.platformType == "js" }
        assertEquals("org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget", jsTarget.target)
        assertEquals(true, jsTarget.extras.js?.isBrowserConfigured)
        assertEquals(true, jsTarget.extras.js?.isNodejsConfigured)
        assertNull(jsTarget.extras.android)
        assertNull(jsTarget.extras.jvm)
        assertNull(jsTarget.extras.native)

        val jvmTarget = metadata.projectTargets.single { it.platformType == "jvm" }
        assertEquals("org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget", jvmTarget.target)
        assertEquals(false, jvmTarget.extras.jvm?.withJavaEnabled)
        assertEquals("1.8", jvmTarget.extras.jvm?.jvmTarget)
        assertNull(jvmTarget.extras.android)
        assertNull(jvmTarget.extras.js)
        assertNull(jvmTarget.extras.native)

        val nativeTarget = metadata.projectTargets.single { it.platformType == "native" }
        assertEquals("org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests", nativeTarget.target)
        val nativeExtras = assertNotNull(nativeTarget.extras.native)
        assertEquals("linux_x64", nativeExtras.konanTarget)
        assertEquals("1.5-dev-17775", nativeExtras.konanVersion)
        assertEquals("1.4.2", nativeExtras.konanAbiVersion)
        assertNull(nativeTarget.extras.android)
        assertNull(nativeTarget.extras.jvm)
        assertNull(nativeTarget.extras.js)
}

}
