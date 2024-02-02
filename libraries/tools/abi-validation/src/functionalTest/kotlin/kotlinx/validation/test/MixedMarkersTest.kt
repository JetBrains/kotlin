/*
 * Copyright 2016-2022 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation.test

import kotlinx.validation.api.*
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
                arguments.add(":apiCheck")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":apiCheck")
        }
    }
}
