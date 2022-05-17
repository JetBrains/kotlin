/*
 * Copyright 2016-2022 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation.test

import kotlinx.validation.api.*
import org.junit.*

class NonPublicMarkersTest : BaseKotlinGradleTest() {

    @Test
    fun testIgnoredMarkersOnProperties() {
        val runner = test {
            buildGradleKts {
                resolve("examples/gradle/base/withPlugin.gradle.kts")
                resolve("examples/gradle/configuration/nonPublicMarkers/markers.gradle.kts")
            }

            kotlin("Properties.kt") {
                resolve("examples/classes/Properties.kt")
            }

            apiFile(projectName = rootProjectDir.name) {
                resolve("examples/classes/Properties.dump")
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
