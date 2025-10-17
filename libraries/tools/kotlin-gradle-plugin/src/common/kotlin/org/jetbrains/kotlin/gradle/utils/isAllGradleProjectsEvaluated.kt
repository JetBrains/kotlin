/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Returns [true] when all gradle projects are evaluated.
 * This flag can be used to ensure that projects can't "contribute" to the Gradle Task Graph
 * e.g. after this state it is relatively safe to resolve configurations.
 * But with Included builds it can be problematic anyway.
 */
internal val Project.isAllGradleProjectsEvaluated get() = isAllGradleProjectsEvaluatedProperty.get()

private val Project.isAllGradleProjectsEvaluatedProperty by projectStoredProperty {
    AtomicBoolean(false)
}

// registration should happen outside task configuration, otherwise gradle will report
// Gradle#projectsEvaluated(Action) on build 'xyz' cannot be executed in the current context.
internal val RegisterIsAllGradleProjectsEvaluatedListener = KotlinProjectSetupAction {
    gradle.projectsEvaluated { isAllGradleProjectsEvaluatedProperty.set(true) }
}