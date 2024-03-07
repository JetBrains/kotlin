/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.resources

import org.gradle.api.Project
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.getOrNull

private const val OVERRIDE_GRADLE_VERSION_FOR_TESTS = "org.jetbrains.kotlin.internal.overriddenGradleVersionForTests"
internal var Project.overriddenGradleVersionForTests: GradleVersion?
    set(value) = extraProperties.set(OVERRIDE_GRADLE_VERSION_FOR_TESTS, value)
    get() = extraProperties.getOrNull(OVERRIDE_GRADLE_VERSION_FOR_TESTS) as? GradleVersion
internal val Project.gradleVersion: GradleVersion get() = overriddenGradleVersionForTests ?: GradleVersion.current()

