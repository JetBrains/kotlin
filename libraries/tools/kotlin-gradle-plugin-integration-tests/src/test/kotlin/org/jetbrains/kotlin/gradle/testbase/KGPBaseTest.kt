/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.test.WithMuteInDatabase
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.streams.asStream

/**
 * Base class for all Kotlin Gradle plugin integration tests.
 */
@Tag("JUnit5")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WithMuteInDatabase
abstract class KGPBaseTest {
    open val defaultBuildOptions = BuildOptions()

    @TempDir
    lateinit var workingDir: Path

    class GradleArgumentsProvider : ArgumentsProvider {
        override fun provideArguments(
            context: ExtensionContext
        ): Stream<out Arguments> {
            var nextSuperclass: Class<*>? = context.testClass.get().superclass
            val superClassSequence = if (nextSuperclass != null) {
                generateSequence {
                    val currentSuperclass = nextSuperclass
                    nextSuperclass = nextSuperclass?.superclass
                    currentSuperclass
                }
            } else {
                emptySequence()
            }

            val versionsAnnotation = sequenceOf(
                context.testMethod.get(),
                context.testClass.get()
            )
                .plus(superClassSequence)
                .mapNotNull { declaration ->
                    declaration.annotations.firstOrNull { it is GradleTestVersions }
                }
                .firstOrNull() as GradleTestVersions?
                ?: context.testMethod.get().annotations
                    .mapNotNull { annotation ->
                        annotation.annotationClass.annotations.firstOrNull { it is GradleTestVersions }
                    }
                    .first() as GradleTestVersions

            val minGradleVersion = GradleVersion.version(versionsAnnotation.minVersion)
            val maxGradleVersion = GradleVersion.version(versionsAnnotation.maxVersion)
            val additionalGradleVersions = versionsAnnotation
                .additionalVersions
                .map(GradleVersion::version)
            additionalGradleVersions.forEach {
                assert(it in minGradleVersion..maxGradleVersion) {
                    "Additional Gradle version ${it.version} should be between ${minGradleVersion.version} and ${maxGradleVersion.version}"
                }
            }

            return setOf(minGradleVersion, *additionalGradleVersions.toTypedArray(), maxGradleVersion)
                .asSequence()
                .map { Arguments.of(it) }
                .asStream()
        }
    }

    @Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class GradleTestVersions(
        val minVersion: String = TestVersions.Gradle.MIN_SUPPORTED,
        val maxVersion: String = TestVersions.Gradle.MAX_SUPPORTED,
        val additionalVersions: Array<String> = []
    )
}