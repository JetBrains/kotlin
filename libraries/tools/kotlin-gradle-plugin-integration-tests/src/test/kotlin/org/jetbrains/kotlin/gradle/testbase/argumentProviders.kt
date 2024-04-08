/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.api.JavaVersion
import org.gradle.util.GradleVersion
import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.jupiter.params.support.AnnotationConsumerInitializer
import org.junit.platform.commons.JUnitException
import org.junit.platform.commons.util.AnnotationUtils
import org.junit.platform.commons.util.ReflectionUtils
import java.io.File
import java.util.stream.Stream
import kotlin.streams.asStream


@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class GradleTestVersions(
    val minVersion: String = TestVersions.Gradle.MIN_SUPPORTED,
    val maxVersion: String = TestVersions.Gradle.MAX_SUPPORTED,
    val additionalVersions: Array<String> = [],
)

/**
 * Parameterized test against different Gradle versions.
 * Test should accept [GradleVersion] as a parameter.
 *
 * By default, [TestVersions.Gradle.MIN_SUPPORTED] and [TestVersions.Gradle.MAX_SUPPORTED] Gradle versions are provided.
 * To modify it use additional [GradleTestVersions] annotation on the test method.
 *
 * @see [GradleTestVersions]
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@GradleTestVersions
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(GradleArgumentsProvider::class)
annotation class GradleTest

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
        context: ExtensionContext,
    ): Stream<out Arguments> {
        val gradleVersions = gradleVersions(context)
        val versionFilter = context.getConfigurationParameter("gradle.integration.tests.gradle.version.filter")
            .map { GradleVersion.version(it) }

        return gradleVersions
            .asSequence()
            .filter { gradleVersion -> versionFilter.map { gradleVersion == it }.orElse(true) }
            .map { Arguments.of(it) }
            .asStream()
    }

    protected fun gradleVersions(context: ExtensionContext): Set<GradleVersion> {
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
    }
}

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class JdkVersions(
    val versions: Array<JavaVersion> = [JavaVersion.VERSION_1_8, JavaVersion.VERSION_21],
    val compatibleWithGradle: Boolean = true,
) {
    class ProvidedJdk(
        val version: JavaVersion,
        val location: File,
    ) {
        override fun toString(): String {
            return "JDK $version"
        }
    }
}

/**
 * Parameterized test against different Gradle and JDK versions.
 * Test should accept [GradleVersion] and [JdkVersions.ProvidedJdk] as a parameters.
 *
 * By default, [TestVersions.Gradle.MIN_SUPPORTED] and [TestVersions.Gradle.MAX_SUPPORTED] Gradle versions are provided.
 * To modify it use additional [GradleTestVersions] annotation on the test method.
 *
 * By default, [JavaVersion.VERSION_1_8] and either maximum compatible with given Gradle release
 * or [JavaVersion.VERSION_21] JDK versions are provided. To modify it use additional [JdkVersions] annotation on the test method.
 *
 * @see [GradleTestVersions]
 * @see [JdkVersions]
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@GradleTestVersions
@JdkVersions
@ParameterizedTest(name = "{1} with {0}: {displayName}")
@ArgumentsSource(GradleAndJdkArgumentsProvider::class)
annotation class GradleWithJdkTest

class GradleAndJdkArgumentsProvider : GradleArgumentsProvider() {
    override fun provideArguments(
        context: ExtensionContext,
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

        val gradleVersions = gradleVersions(context)
        val versionFilter = context.getConfigurationParameter("gradle.integration.tests.gradle.version.filter")
            .map { GradleVersion.version(it) }

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
            .filter { (gradleVersion, _) -> versionFilter.map { gradleVersion == it }.orElse(true) }
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
    val additionalVersions: Array<String> = [],
)

/**
 * Parameterized test against different Android Gradle plugin versions.
 * Test should accept [GradleVersion], [String] (AGP version) and [JdkVersions.ProvidedJdk] as a parameters.
 *
 * By default, [TestVersions.AGP.MIN_SUPPORTED] and [TestVersions.AGP.MAX_SUPPORTED] AGP versions are provided.
 * To modify it use additional [AndroidTestVersions] annotation on the test method.
 *
 * @see [AndroidTestVersions]
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@GradleTestVersions
@AndroidTestVersions
@ParameterizedTest(name = "AGP {1} with {0}: {displayName}")
@ArgumentsSource(GradleAndAgpArgumentsProvider::class)
annotation class GradleAndroidTest

class GradleAndAgpArgumentsProvider : GradleArgumentsProvider() {
    override fun provideArguments(
        context: ExtensionContext,
    ): Stream<out Arguments> {
        val agpVersionsAnnotation = findAnnotation<AndroidTestVersions>(context)
        val agpVersions = setOfNotNull(
            agpVersionsAnnotation.minVersion,
            *agpVersionsAnnotation.additionalVersions,
            if (agpVersionsAnnotation.minVersion < agpVersionsAnnotation.maxVersion) agpVersionsAnnotation.maxVersion else null
        )

        val gradleVersions = gradleVersions(context)
        val versionFilter = context.getConfigurationParameter("gradle.integration.tests.gradle.version.filter")
            .map { GradleVersion.version(it) }

        return agpVersions
            .flatMap { version ->
                val agpVersion = TestVersions.AgpCompatibilityMatrix.entries.find { it.version == version }
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
            .filter { agpTestArguments -> versionFilter.map { agpTestArguments.gradleVersion == it }.orElse(true) }
            .map {
                Arguments.of(it.gradleVersion, it.agpVersion, it.jdkVersion)
            }
            .asStream()
    }

    data class AgpTestArguments(
        val gradleVersion: GradleVersion,
        val agpVersion: String,
        val jdkVersion: JdkVersions.ProvidedJdk,
    )
}

/**
 * Disables a parametrized test if any of argument providers doesn't have arguments to provide.
 * When gradle.integration.tests.gradle.version.filter property is used, all arguments of a GradleArgumentsProvider may be filtered out.
 * If such a test is not disabled, it will fail with initialization error.
 */
class DisabledIfNoArgumentsProvided : ExecutionCondition {
    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
        if (!context.testMethod.isPresent) {
            return ConditionEvaluationResult.enabled("The execution condition is only applicable to test methods")
        }

        val gradleVersionFilterParameter = context.getConfigurationParameter("gradle.integration.tests.gradle.version.filter")
        if (!gradleVersionFilterParameter.isPresent) {
            return ConditionEvaluationResult.enabled("No Gradle version filter provided")
        }

        val argumentProviders = AnnotationUtils.findRepeatableAnnotations(context.requiredTestMethod, ArgumentsSource::class.java)
            .map(ArgumentsSource::value)
            .map { instantiateArgumentsProvider(it.java) }
            .map { provider -> AnnotationConsumerInitializer.initialize(context.requiredTestMethod, provider) }

        return if (argumentProviders.any { it.provideArguments(context).count() == 0L })
            ConditionEvaluationResult.disabled("No arguments provided")
        else
            ConditionEvaluationResult.enabled("Arguments provided")
    }

    private fun instantiateArgumentsProvider(clazz: Class<out ArgumentsProvider>): ArgumentsProvider {
        try {
            return ReflectionUtils.newInstance(clazz)
        } catch (ex: Exception) {
            if (ex is NoSuchMethodException) {
                val message = String.format(
                    "Failed to find a no-argument constructor for ArgumentsProvider [%s]. "
                            + "Please ensure that a no-argument constructor exists and "
                            + "that the class is either a top-level class or a static nested class",
                    clazz.getName()
                )
                throw JUnitException(message, ex)
            }
            throw ex
        }
    }
}
