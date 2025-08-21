/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.abi.tools.test.legacy

import org.jetbrains.abi.tools.test.api.*
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
                arguments.add(":updateLegacyAbi")
            }
        }
        runner.build().apply {
            assertTaskSuccess(":updateLegacyAbi")

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
                arguments.add(":checkLegacyAbi")
            }
        }
        runner.build().apply {
            assertTaskSuccess(":checkLegacyAbi")
        }
    }
}
