/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.jvm

import org.gradle.internal.component.external.model.TestFixturesSupport.TEST_FIXTURES_FEATURE_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaTarget
import org.jetbrains.kotlin.gradle.targets.KotlinTargetSideEffect
import org.jetbrains.kotlin.gradle.utils.javaSourceSets

private const val JAVA_TEST_FIXTURES_PLUGIN_ID = "java-test-fixtures"
internal val ConfigureJavaTestFixturesSideEffect = KotlinTargetSideEffect { target ->
    if (target !is KotlinJvmTarget && target !is KotlinWithJavaTarget<*, *>) return@KotlinTargetSideEffect

    target.project.plugins.withId(JAVA_TEST_FIXTURES_PLUGIN_ID) {
        val testFixturesSourceSet = target.project.javaSourceSets.findByName(TEST_FIXTURES_FEATURE_NAME)
        if (testFixturesSourceSet == null) {
            target.project.logger.warn(
                "The `$JAVA_TEST_FIXTURES_PLUGIN_ID` plugin has been detected, " +
                        "however the `$TEST_FIXTURES_FEATURE_NAME` source set cannot be found. " +
                        "`internal` declarations can be not available in the test fixtures.",
            )
            return@withId
        }
        val testFixturesCompilation = target.compilations.maybeCreate(testFixturesSourceSet.name)
        val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
        val testCompilation = target.compilations.getByName(KotlinCompilation.TEST_COMPILATION_NAME)
        testFixturesCompilation.associateWith(mainCompilation)
        testCompilation.associateWith(testFixturesCompilation)
        val defaultSourceSetNames = setOf(mainCompilation.defaultSourceSet.name, testFixturesCompilation.defaultSourceSet.name)
            .joinToString(separator = ", ", prefix = "'", postfix = "'")
        target.project.logger.debug(
            "The `$JAVA_TEST_FIXTURES_PLUGIN_ID` plugin has been detected, and the `$TEST_FIXTURES_FEATURE_NAME` " +
                    "source set has been associated with the $defaultSourceSetNames source sets to provide `internal` declarations access."
        )
    }
}