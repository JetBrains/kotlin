/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.HierarchicalStructureOptInMigrationArtifactContentMppIT.Mode.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.zip.ZipFile
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(Parameterized::class)
internal class HierarchicalStructureOptInMigrationArtifactContentMppIT : BaseGradleIT() {
    override val defaultGradleVersion: GradleVersionRequired = GradleVersionRequired.FOR_MPP_SUPPORT

    enum class Mode {
        HMPP_BY_DEFAULT, OPT_OUT_HMPP, DISABLE_HMPP_BY_DEFAULT
    }

    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun params() = Mode.values().map { arrayOf(it) }
    }

    @Parameterized.Parameter(0)
    lateinit var mode: Mode

    @ExperimentalStdlibApi
    @Test
    @Suppress("NON_EXHAUSTIVE_WHEN")
    fun testArtifactFormatAndContent() = with(transformProjectWithPluginsDsl("new-mpp-published")) {
        projectDir.resolve("gradle.properties").delete()

        build(
            *buildList {
                add("clean")
                add("publish")
                add("-Pkotlin.internal.suppressGradlePluginErrors=PreHMPPFlagsError")
                when (mode) {
                    OPT_OUT_HMPP, HMPP_BY_DEFAULT -> {}
                    DISABLE_HMPP_BY_DEFAULT -> { add("-Pkotlin.internal.mpp.hierarchicalStructureByDefault=false") }
                }
                when (mode) {
                    OPT_OUT_HMPP -> add("-Pkotlin.mpp.hierarchicalStructureSupport=false")
                    HMPP_BY_DEFAULT, DISABLE_HMPP_BY_DEFAULT -> {}
                }
            }.toTypedArray(),
        ) {
            assertSuccessful()
            val metadataJarEntries = ZipFile(
                projectDir.resolve("../repo/com/example/bar/my-lib-bar/1.0/my-lib-bar-1.0.jar")
            ).use { zip ->
                zip.entries().asSequence().toList().map { it.name }
            }

            if (mode != DISABLE_HMPP_BY_DEFAULT) {
                assertTrue { metadataJarEntries.any { "commonMain" in it } }
            }

            val hasJvmAndJsMainEntries = metadataJarEntries.any { "jvmAndJsMain" in it }
            val shouldHaveJvmAndJsMainEntries = when (mode) {
                OPT_OUT_HMPP, DISABLE_HMPP_BY_DEFAULT -> false
                HMPP_BY_DEFAULT -> true
            }
            assertEquals(shouldHaveJvmAndJsMainEntries, hasJvmAndJsMainEntries)
        }
    }
}