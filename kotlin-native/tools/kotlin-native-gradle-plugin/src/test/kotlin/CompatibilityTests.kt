/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.plugin.test

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertTrue

open class CompatibilityTests {
    val tmpFolder = TemporaryFolder()
        @Rule get

    val projectDirectory: File
        get() = tmpFolder.root

    @Test
    fun `Plugin should fail if running with Gradle prior to the required one`() {
        val project = KonanProject.createEmpty(projectDirectory)
        val result = project
                .createRunner()
                .withGradleDistribution(URI.create(
                    "https://cache-redirector.jetbrains.com/services.gradle.org/distributions/gradle-4.5-bin.zip"
                ))
                .withArguments("tasks")
                .buildAndFail()
        println(result.output)
        assertTrue("Build doesn't show the warning message") {
            result.output.contains("Kotlin/Native Gradle plugin is incompatible with this version of Gradle.")
        }
    }
}