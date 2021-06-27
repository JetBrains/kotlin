/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.junit.jupiter.api.Tag

/**
 * Add it to test classes performing simple KGP checks.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag("SimpleKGP")
annotation class SimpleGradlePluginTests

/**
 * Add it to test classes performing Gradle or Kotlin daemon checks.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Tag("DaemonsKGP")
annotation class DaemonsGradlePluginTests
