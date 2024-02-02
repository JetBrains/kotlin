/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsSource
import java.nio.file.Path
import java.util.stream.Stream
import java.util.zip.ZipFile
import kotlin.io.path.deleteIfExists
import kotlin.streams.asStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.streams.toList

@MppGradlePluginTests
@DisplayName("Hierarchical multiplatform publication artifact content")
internal class HierarchicalStructureOptInMigrationArtifactContentMppIT : KGPBaseTest() {

    @GradleWithHmppModeTest
    @DisplayName("Artifact format and content conformance")
    fun testArtifactFormatAndContent(
        gradleVersion: GradleVersion,
        hmppMode: Mode,
        @TempDir localRepository: Path,
    ) {
        project(
            projectName = "new-mpp-published",
            gradleVersion = gradleVersion,
            localRepoDir = localRepository,
        ) {
            gradleProperties.deleteIfExists()

            val hierarchicalStructureFlag = when (hmppMode) {
                Mode.OPT_OUT_HMPP, Mode.HMPP_BY_DEFAULT -> null
                Mode.DISABLE_HMPP_BY_DEFAULT -> "-Pkotlin.internal.mpp.hierarchicalStructureByDefault=false"
            }
            val hierarchicalStructureSupportFlag = when(hmppMode) {
                Mode.OPT_OUT_HMPP -> "-Pkotlin.mpp.hierarchicalStructureSupport=false"
                Mode.HMPP_BY_DEFAULT, Mode.DISABLE_HMPP_BY_DEFAULT -> null
            }

            build(
                buildArguments = listOfNotNull(
                    "clean",
                    "publish",
                    "-Pkotlin.internal.suppressGradlePluginErrors=PreHMPPFlagsError",
                    hierarchicalStructureFlag,
                    hierarchicalStructureSupportFlag
                ).toTypedArray()
            ) {
                val metadataJarEntries = ZipFile(
                    localRepository.resolve("com/example/bar/my-lib-bar/1.0/my-lib-bar-1.0.jar").toFile()
                ).use { zip ->
                    zip.entries().asSequence().toList().map { it.name }
                }

                if (hmppMode != Mode.DISABLE_HMPP_BY_DEFAULT) {
                    assertTrue { metadataJarEntries.any { "commonMain" in it } }
                }

                val hasJvmAndJsMainEntries = metadataJarEntries.any { "jvmAndJsMain" in it }
                val shouldHaveJvmAndJsMainEntries = when (hmppMode) {
                    Mode.OPT_OUT_HMPP, Mode.DISABLE_HMPP_BY_DEFAULT -> false
                    Mode.HMPP_BY_DEFAULT -> true
                }
                assertEquals(shouldHaveJvmAndJsMainEntries, hasJvmAndJsMainEntries)
            }
        }
    }

    enum class Mode {
        HMPP_BY_DEFAULT, OPT_OUT_HMPP, DISABLE_HMPP_BY_DEFAULT
    }

    class GradleAndHmppModeProvider : GradleArgumentsProvider() {
        override fun provideArguments(
            context: ExtensionContext
        ): Stream<out Arguments> {
            val gradleVersions = super.provideArguments(context).map { it.get().first() as GradleVersion }.toList()

            return gradleVersions
                .flatMap { gradleVersion ->
                    Mode.entries.map {
                        Arguments.of(gradleVersion, it)
                    }
                }
                .asSequence()
                .asStream()
        }
    }

    @Target(AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.RUNTIME)
    @GradleTestVersions
    @ParameterizedTest(name = "{0} mode {1}: {displayName}")
    @ArgumentsSource(GradleAndHmppModeProvider::class)
    annotation class GradleWithHmppModeTest
}