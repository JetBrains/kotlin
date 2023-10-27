/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.sample

import org.jetbrains.dokka.analysis.test.api.kotlinJvmTestProject
import org.jetbrains.dokka.analysis.test.api.useServices
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SampleAnalysisTest {

    @Test
    fun `should return sources of a kotlin sample`() {
        val testProject = kotlinJvmTestProject {
            dokkaConfiguration {
                kotlinSourceSet {
                    additionalSourceRoots = setOf("/samples")
                }
            }
            sampleFile("/samples/stringListOf-sample.kt", fqPackageName = "org.jetbrains.dokka.sample.generator") {
                +"""
                    import org.jetbrains.dokka.DokkaConfiguration
                    import org.jetbrains.dokka.DokkaGenerator
                    import org.jetbrains.dokka.utilities.DokkaLogger
                    
                    fun runGenerator(configuration: DokkaConfiguration, logger: DokkaLogger) {
                        DokkaGenerator(configuration, logger).generate()
                    }
                """
            }
        }

        testProject.useServices { context ->
            val sampleSourceSet = context.configuration.sourceSets.single()

            val sampleProvider = sampleProviderFactory.build()
            val sample = sampleProvider.getSample(sampleSourceSet, "org.jetbrains.dokka.sample.generator.runGenerator")
            assertNotNull(sample)

            val expectedImports = listOf(
                "import org.jetbrains.dokka.DokkaConfiguration",
                "import org.jetbrains.dokka.DokkaGenerator",
                "import org.jetbrains.dokka.utilities.DokkaLogger"
            ).joinToString(separator = "\n")

            val expectedBody = "DokkaGenerator(configuration, logger).generate()"

            assertEquals(expectedImports, sample.imports)
            assertEquals(expectedBody, sample.body)
        }
    }
}
