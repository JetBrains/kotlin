/*
 * Copyright 2016-2022 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation.test

import kotlinx.validation.api.*
import kotlinx.validation.api.buildGradleKts
import kotlinx.validation.api.kotlin
import kotlinx.validation.api.resolve
import kotlinx.validation.api.test
import org.junit.Test

class PublicMarkersTest : BaseKotlinGradleTest() {

    @Test
    fun testPublicMarkers() {
        val runner = test {
            buildGradleKts {
                resolve("/examples/gradle/base/withPlugin.gradle.kts")
                resolve("/examples/gradle/configuration/publicMarkers/markers.gradle.kts")
            }

            kotlin("ClassWithPublicMarkers.kt") {
                resolve("/examples/classes/ClassWithPublicMarkers.kt")
            }

            kotlin("ClassInPublicPackage.kt") {
                resolve("/examples/classes/ClassInPublicPackage.kt")
            }

            apiFile(projectName = rootProjectDir.name) {
                resolve("/examples/classes/ClassWithPublicMarkers.dump")
            }

            runner {
                arguments.add(":apiCheck")
            }
        }

        runner.withDebug(true).build().apply {
            assertTaskSuccess(":apiCheck")
        }
    }
}
