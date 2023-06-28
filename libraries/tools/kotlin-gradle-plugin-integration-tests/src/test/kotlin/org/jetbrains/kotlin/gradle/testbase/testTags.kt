/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.junit.jupiter.api.Tag

/**
 * Add it to test classes performing Gradle or Kotlin daemon checks.
 *
 * You could add tag onto test suite once, but then all tests
 * in test suite should be for the related tag.
 * Preferably add tag for each test.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag("DaemonsKGP")
annotation class DaemonsGradlePluginTests

/**
 * Add it to tests covering Kotlin Gradle Plugin/JVM platform.
 *
 * You could add tag onto test suite once, but then all tests
 * in test suite should be for the related tag.
 * Preferably add tag for each test.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag("JvmKGP")
annotation class JvmGradlePluginTests

/**
 * Add it to tests covering Kotlin Gradle Plugin/JS platform.
 *
 * You could add tag onto test suite once, but then all tests
 * in test suite should be for the related tag.
 * Preferably add tag for each test.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag("JsKGP")
annotation class JsGradlePluginTests

/**
 * Add it to tests covering Kotlin Gradle Plugin/Native platform.
 *
 * You could add tag onto test suite once, but then all tests
 * in test suite should be for the related tag.
 * Preferably add tag for each test.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag("NativeKGP")
annotation class NativeGradlePluginTests

/**
 * Add it to tests covering Kotlin Multiplatform Gradle plugin.
 *
 * You could add tag onto test suite once, but then all tests
 * in test suite should be for the related tag.
 * Preferably add tag for each test.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag("MppKGP")
annotation class MppGradlePluginTests

/**
 * Add it to the tests covering Kotlin Android Gradle plugin.
 *
 * You could add tag onto test suite once, but then all tests
 * in test suite should be for the related tag.
 * Preferably add tag for each test.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag("AndroidKGP")
annotation class AndroidGradlePluginTests

/**
 * Add it the tests that are not covered by tags above.
 *
 * Usually it would be tests for kapt, serialization plugins, etc...
 *
 * You could add tag onto test suite once, but then all tests
 * in test suite should be for the related tag.
 * Preferably add tag for each test.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag("OtherKGP")
annotation class OtherGradlePluginTests

