/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling

import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.intellij.lang.annotations.Language
import org.junit.Test
import kotlin.test.assertEquals

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

class DeserializeStringTest {

    @Test
    fun sample1() {
        @Language("JSON") val json =
            """
            {
              "buildSystem": "Gradle",
              "buildSystemVersion": "6.7",
              "buildPlugin": "org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper",
              "buildPluginVersion": "1.5.255-SNAPSHOT",
              "projectSettings": {
                "isHmppEnabled": false,
                "isCompatibilityMetadataVariantEnabled": true
              },
              "projectTargets": [
                {
                  "target": "org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget",
                  "platformType": "androidJvm",
                  "extras": {
                    "sourceCompatibility": "1.7",
                    "targetCompatibility": "1.7"
                  }
                },
                {
                  "target": "org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget_Decorated",
                  "platformType": "js",
                  "extras": {
                    "isBrowserConfigured": "true",
                    "isNodejsConfigured": "true"
                  }
                },
                {
                  "target": "org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget_Decorated",
                  "platformType": "jvm",
                  "extras": {
                    "withJavaEnabled": "false",
                    "jvmTarget": "1.8"
                  }
                },
                {
                  "target": "org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget_Decorated",
                  "platformType": "common"
                }
              ]
            }
            """.trimIndent()

        val metadata = KotlinToolingMetadata.parseJsonOrThrow(json)
        assertEquals("Gradle", metadata.buildSystem)
        assertEquals("6.7", metadata.buildSystemVersion)
        assertEquals("org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper", metadata.buildPlugin)
        assertEquals("1.5.255-SNAPSHOT", metadata.buildPluginVersion)
        assertFalse(metadata.projectSettings.isHmppEnabled)
        assertTrue(metadata.projectSettings.isCompatibilityMetadataVariantEnabled)
        assertEquals(4, metadata.projectTargets.size, "Expected exactly 4 targets")

        val androidJvmTarget = metadata.projectTargets.single { it.platformType == "androidJvm" }
        assertEquals("org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget", androidJvmTarget.target)
        assertEquals(2, androidJvmTarget.extras.size, "Expected exactly two extras")
        assertEquals("1.7", androidJvmTarget.extras["sourceCompatibility"])
        assertEquals("1.7", androidJvmTarget.extras["targetCompatibility"])

        val jsTarget = metadata.projectTargets.single { it.platformType == "js" }
        assertEquals("org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget_Decorated", jsTarget.target)
        assertEquals(2, jsTarget.extras.size, "Expected exactly two extras")
        assertEquals("true", jsTarget.extras["isBrowserConfigured"])
        assertEquals("true", jsTarget.extras["isNodejsConfigured"])

        val jvmTarget = metadata.projectTargets.single { it.platformType == "jvm" }
        assertEquals("org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget_Decorated", jvmTarget.target)
        assertEquals(2, jvmTarget.extras.size, "Expected exactly two extras")
        assertEquals("false", jvmTarget.extras["withJavaEnabled"])
        assertEquals("1.8", jvmTarget.extras["jvmTarget"])

        val commonTarget = metadata.projectTargets.single { it.platformType == "common" }
        assertEquals("org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget_Decorated", commonTarget.target)
        assertEquals(0, commonTarget.extras.size, "Expected zero extras")
    }
}

