/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation.test

import kotlinx.validation.api.*
import kotlinx.validation.api.resolve
import kotlinx.validation.api.test
import org.assertj.core.api.Assertions
import org.junit.Test

class JvmProjectTests : BaseKotlinGradleTest() {
    @Test
    fun `apiDump for a project with generated sources only`() {
        val runner = test {
            buildGradleKts {
                resolve("/examples/gradle/base/withPlugin.gradle.kts")
                resolve("/examples/gradle/configuration/generatedSources/generatedJvmSources.gradle.kts")
            }
            runner {
                arguments.add(":apiDump")
            }
        }
        runner.build().apply {
            assertTaskSuccess(":apiDump")

            val expectedApi = readFileList("/examples/classes/GeneratedSources.dump")
            Assertions.assertThat(rootProjectApiDump.readText()).isEqualToIgnoringNewLines(expectedApi)
        }
    }

    @Test
    fun `apiCheck for a project with generated sources only`() {
        val runner = test {
            buildGradleKts {
                resolve("/examples/gradle/base/withPlugin.gradle.kts")
                resolve("/examples/gradle/configuration/generatedSources/generatedJvmSources.gradle.kts")
            }
            apiFile(projectName = rootProjectDir.name) {
                resolve("/examples/classes/GeneratedSources.dump")
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
