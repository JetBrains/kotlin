/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.gradle.api.Project

/**
 * Executes [test] with the Gradle property [propertyName] set to [value] (or cleared, if [value] is `null`), restoring
 * the previous value (or absence of it) afterward.
 *
 * `ProviderFactory.gradleProperty` doesn't pick up values set via [Project]'s extra properties or a `gradle.properties`
 * file written into a [org.gradle.testfixtures.ProjectBuilder]-based test project, so a system property is used instead,
 * following the convention Gradle properties use to look up their value (`org.gradle.project.<name>`).
 */
internal fun withGradleProperty(propertyName: String, value: String?, test: () -> Unit) {
    val systemPropertyName = "org.gradle.project.$propertyName"
    val previousValue = System.getProperty(systemPropertyName)
    if (value != null) System.setProperty(systemPropertyName, value) else System.clearProperty(systemPropertyName)
    try {
        test()
    } finally {
        if (previousValue != null) System.setProperty(systemPropertyName, previousValue) else System.clearProperty(systemPropertyName)
    }
}

/**
 * Returns the [org.gradle.api.services.BuildServiceSpec.getMaxParallelUsages] configured for the
 * `KotlinNativeToolParallelismLimitService` registered under [taskName], or `null` if no limit was set.
 */
internal fun Project.maxParallelUsagesOf(taskName: String): Int? =
    gradle.sharedServices.registrations.getByName("KotlinNativeToolParallelismLimitService$taskName").maxParallelUsages.orNull
