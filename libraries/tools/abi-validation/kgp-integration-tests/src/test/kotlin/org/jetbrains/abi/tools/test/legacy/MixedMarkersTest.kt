/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.abi.tools.test.legacy

import org.jetbrains.abi.tools.test.api.*
import org.junit.Test

class MixedMarkersTest : BaseKotlinGradleTest() {

    @Test
    fun testMixedMarkers() {
        val runner = test {
            buildGradleKts {
                resolve("/examples/gradle/base/withPlugin.gradle.kts")
                resolve("/examples/gradle/configuration/publicMarkers/mixedMarkers.gradle.kts")
            }

            kotlin("MixedAnnotations.kt") {
                resolve("/examples/classes/MixedAnnotations.kt")
            }

            apiFile(projectName = rootProjectDir.name) {
                resolve("/examples/classes/MixedAnnotations.dump")
            }

            runner {
                arguments.add(":checkLegacyAbi")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":checkLegacyAbi")
        }
    }
}
