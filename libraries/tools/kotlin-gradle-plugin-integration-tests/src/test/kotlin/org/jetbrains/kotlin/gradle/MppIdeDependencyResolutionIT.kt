/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.commonizer.identityString
import org.jetbrains.kotlin.gradle.idea.proto.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinSerializationContext
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinSerializationLogger
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency.Companion.CLASSPATH_BINARY_TYPE
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.extras.isCommonized
import org.jetbrains.kotlin.gradle.idea.tcs.extras.isNativeDistribution
import org.jetbrains.kotlin.gradle.idea.tcs.extras.isNativeStdlib
import org.jetbrains.kotlin.gradle.idea.tcs.extras.klibExtra
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.assertMatches
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.binaryCoordinates
import org.jetbrains.kotlin.gradle.plugin.ide.kotlinExtrasSerialization
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.konan.target.KonanTarget.*
import org.junit.jupiter.api.DisplayName
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.fail

@MppGradlePluginTests
@DisplayName("Multiplatform IDE dependency resolution")
class MppIdeDependencyResolutionIT : KGPBaseTest() {
    @GradleTest
    fun testCommonizedPlatformDependencyResolution(gradleVersion: GradleVersion) {
        with(project("commonizeHierarchically", gradleVersion)) {
            resolveIdeDependencies(":p1") { dependencies ->
                if (task(":commonizeNativeDistribution") == null) fail("Missing :commonizeNativeDistribution task")

                fun Iterable<IdeaKotlinDependency>.filterNativePlatformDependencies() =
                    filterIsInstance<IdeaKotlinResolvedBinaryDependency>()
                        .filter { !it.isNativeStdlib }
                        .filter { it.isNativeDistribution }
                        .filter { it.binaryType == CLASSPATH_BINARY_TYPE }

                val nativeMainDependencies = dependencies["nativeMain"].filterNativePlatformDependencies()
                val nativeTestDependencies = dependencies["nativeTest"].filterNativePlatformDependencies()
                val linuxMainDependencies = dependencies["linuxMain"].filterNativePlatformDependencies()
                val linuxTestDependencies = dependencies["linuxTest"].filterNativePlatformDependencies()

                /* Check test and main receive the same dependencies */
                run {
                    nativeMainDependencies.assertMatches(nativeTestDependencies)
                    linuxMainDependencies.assertMatches(linuxTestDependencies)
                }

                /* Check all dependencies are marked as commonized and commonizer target match */
                run {
                    nativeMainDependencies.plus(linuxMainDependencies).forEach { dependency ->
                        if (!dependency.isCommonized) fail("$dependency is not marked as 'isCommonized'")
                    }

                    val nativeMainTarget = CommonizerTarget(
                        LINUX_X64, LINUX_ARM64, MACOS_X64, MACOS_ARM64, IOS_X64, IOS_ARM64, IOS_SIMULATOR_ARM64, MINGW_X64, MINGW_X86
                    )

                    nativeMainDependencies.forEach { dependency ->
                        assertEquals(nativeMainTarget.identityString, dependency.klibExtra?.commonizerTarget)
                    }

                    val linuxMainTarget = CommonizerTarget(LINUX_X64, LINUX_ARM64)
                    linuxMainDependencies.forEach { dependency ->
                        assertEquals(linuxMainTarget.identityString, dependency.klibExtra?.commonizerTarget)
                    }
                }

                /* Find posix library */
                run {
                    nativeMainDependencies.assertMatches(
                        binaryCoordinates(Regex("org\\.jetbrains\\.kotlin\\.native:platform.posix:.*")),
                        binaryCoordinates(Regex("org\\.jetbrains\\.kotlin\\.native:platform.*"))
                    )

                    linuxMainDependencies.assertMatches(
                        binaryCoordinates(Regex("org\\.jetbrains\\.kotlin\\.native:platform.posix:.*")),
                        binaryCoordinates(Regex("org\\.jetbrains\\.kotlin\\.native:platform.*"))
                    )
                }
            }
        }
    }
}

/* Test Utils / Test Infrastructure Implementation */

private fun TestProject.resolveIdeDependencies(
    subproject: String? = null,
    assertions: BuildResult.(dependencies: IdeaKotlinDependenciesContainer) -> Unit
) {
    build("${subproject.orEmpty()}:resolveIdeDependencies") {
        val subprojectPathPrefix = subproject?.removePrefix(":")?.replace(":", "/")?.plus("/") ?: ""
        val output = projectPath.resolve("${subprojectPathPrefix}build/ide/dependencies/proto").toFile()
        if (!output.isDirectory) fail("Missing output directory: $output")

        val dependenciesBySourceSetName = output.listFiles().orEmpty().associate { sourceSetDirectory ->
            if (!sourceSetDirectory.isDirectory) fail("Expected $sourceSetDirectory to be directory")
            val serializedDependencyFiles = sourceSetDirectory.listFiles().orEmpty()
            val deserializedDependencies = serializedDependencyFiles.map { dependencyFile ->
                deserializeIdeaKotlinDependencyOrFail(dependencyFile)
            }

            sourceSetDirectory.name to deserializedDependencies.toSet()
        }

        assertions(IdeaKotlinDependenciesContainer(dependenciesBySourceSetName))
    }
}

private fun deserializeIdeaKotlinDependencyOrFail(file: File): IdeaKotlinDependency {
    return GradleIntegrationTestIdeaKotlinSerializationContext.IdeaKotlinDependency(file.readBytes())
        ?: fail("Failed to deserialize dependency. $file")
}

private object GradleIntegrationTestIdeaKotlinSerializationContext : IdeaKotlinSerializationContext {
    override val extrasSerializationExtension = kotlinExtrasSerialization
    override val logger: IdeaKotlinSerializationLogger = object : IdeaKotlinSerializationLogger {
        override fun report(severity: IdeaKotlinSerializationLogger.Severity, message: String, cause: Throwable?) {
            println("$severity: $message")
            if (cause != null) println(cause.stackTraceToString())
        }
    }
}

private class IdeaKotlinDependenciesContainer(
    private val dependencies: Map<String, Set<IdeaKotlinDependency>>
) {
    operator fun get(sourceSetName: String) = dependencies[sourceSetName]
        ?: fail("SourceSet with name $sourceSetName not found. Found: ${dependencies.keys}")
}
