/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.api.JavaVersion
import org.gradle.util.GradleVersion
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.io.File
import java.util.stream.Stream
import kotlin.streams.asStream
import kotlin.streams.toList

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class GradleTestVersions(
    val minVersion: String = TestVersions.Gradle.MIN_SUPPORTED,
    val maxVersion: String = TestVersions.Gradle.MAX_SUPPORTED,
    val additionalVersions: Array<String> = []
)

inline fun <reified T : Annotation> findAnnotation(context: ExtensionContext): T {
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

    return sequenceOf(
        context.testMethod.orElse(null),
        context.testClass.orElse(null)
    )
        .filterNotNull()
        .plus(superClassSequence)
        .mapNotNull { declaration ->
            declaration.annotations.firstOrNull { it is T }
        }
        .firstOrNull() as T?
        ?: context.testMethod.get().annotations
            .mapNotNull { annotation ->
                annotation.annotationClass.annotations.firstOrNull { it is T }
            }
            .first() as T
}

open class GradleArgumentsProvider : ArgumentsProvider {
    override fun provideArguments(
        context: ExtensionContext
    ): Stream<out Arguments> {
        val versionsAnnotation = findAnnotation<GradleTestVersions>(context)

        fun max(a: GradleVersion, b: GradleVersion) = if (a >= b) a else b
        val minGradleVersion = GradleVersion.version(versionsAnnotation.minVersion)
        // Max is used for cases when test is annotated with `@GradleTestVersions(minVersion = LATEST)` but MAX_SUPPORTED isn't latest
        val maxGradleVersion = max(GradleVersion.version(versionsAnnotation.maxVersion), minGradleVersion)

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
annotation class JdkVersions(
    val versions: Array<JavaVersion> = [JavaVersion.VERSION_1_8, JavaVersion.VERSION_21],
    val compatibleWithGradle: Boolean = true
) {
    class ProvidedJdk(
        val version: JavaVersion,
        val location: File
    ) {
        override fun toString(): String {
            return "JDK $version"
        }
    }
}

class GradleAndJdkArgumentsProvider : GradleArgumentsProvider() {
    override fun provideArguments(
        context: ExtensionContext
    ): Stream<out Arguments> {
        val jdkAnnotation = findAnnotation<JdkVersions>(context)
        val providedJdks = jdkAnnotation
            .versions
            .map {
                JdkVersions.ProvidedJdk(
                    it,
                    File(System.getProperty("jdk${it.majorVersion}Home"))
                )
            }

        val gradleVersions = super.provideArguments(context).map { it.get().first() as GradleVersion }.toList()

        return providedJdks
            .flatMap { providedJdk ->
                val minSupportedGradleVersion = jdkGradleCompatibilityMatrix[providedJdk.version]
                gradleVersions
                    .run {
                        if (jdkAnnotation.compatibleWithGradle && minSupportedGradleVersion != null) {
                            val initialVersionsCount = count()
                            val filteredVersions = filter { it >= minSupportedGradleVersion }
                            if (initialVersionsCount > filteredVersions.count()) {
                                (filteredVersions + minSupportedGradleVersion).toSet()
                            } else {
                                filteredVersions
                            }
                        } else this
                    }
                    .map { it to providedJdk }
            }
            .asSequence()
            .map {
                Arguments.of(it.first, it.second)
            }
            .asStream()
    }

    companion object {
        private val jdkGradleCompatibilityMatrix = mapOf(
            JavaVersion.VERSION_16 to GradleVersion.version(TestVersions.Gradle.G_7_0),
            JavaVersion.VERSION_17 to GradleVersion.version(TestVersions.Gradle.G_7_3),
            JavaVersion.VERSION_21 to GradleVersion.version(TestVersions.Gradle.G_7_3),
        )
    }
}

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AndroidTestVersions(
    val minVersion: String = TestVersions.AGP.MIN_SUPPORTED,
    val maxVersion: String = TestVersions.AGP.MAX_SUPPORTED,
    val additionalVersions: Array<String> = []
)

class GradleAndAgpArgumentsProvider : GradleArgumentsProvider() {
    override fun provideArguments(
        context: ExtensionContext
    ): Stream<out Arguments> {
        val agpVersionsAnnotation = findAnnotation<AndroidTestVersions>(context)
        val agpVersions = setOfNotNull(
            agpVersionsAnnotation.minVersion,
            *agpVersionsAnnotation.additionalVersions,
            if (agpVersionsAnnotation.minVersion < agpVersionsAnnotation.maxVersion) agpVersionsAnnotation.maxVersion else null
        )

        val gradleVersions = super.provideArguments(context).map { it.get().first() as GradleVersion }.toList()

        return agpVersions
            .flatMap { version ->
                val agpVersion = TestVersions.AgpCompatibilityMatrix.values().find { it.version == version }
                    ?: throw IllegalArgumentException("AGP version $version is not defined in TestVersions.AGP!")

                val providedJdk = JdkVersions.ProvidedJdk(
                    agpVersion.requiredJdkVersion,
                    File(System.getProperty("jdk${agpVersion.requiredJdkVersion.majorVersion}Home"))
                )

                gradleVersions
                    .filter { it in agpVersion.minSupportedGradleVersion..agpVersion.maxSupportedGradleVersion }
                    .ifEmpty {
                        // Falling back to the minimal supported Gradle version for this AGP version
                        listOf(agpVersion.minSupportedGradleVersion)
                    }
                    .map {
                        AgpTestArguments(it, agpVersion.version, providedJdk)
                    }
            }
            .asSequence()
            .map {
                Arguments.of(it.gradleVersion, it.agpVersion, it.jdkVersion)
            }
            .asStream()
    }

    data class AgpTestArguments(
        val gradleVersion: GradleVersion,
        val agpVersion: String,
        val jdkVersion: JdkVersions.ProvidedJdk
    )
}
