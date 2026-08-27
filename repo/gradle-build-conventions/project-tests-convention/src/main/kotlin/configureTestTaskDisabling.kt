/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.kotlin.dsl.withType

internal fun Project.configureTestTaskDisabling() {
    tasks.withType<AbstractTestTask>().configureEach {
        val disableVerificationTasks: Provider<Boolean> = providers.gradleProperty("kotlin.build.disable.verification.tasks")
            .map { it.toBoolean() }
            .orElse(false)
        inputs.property("kotlin.build.disable.verification.tasks", disableVerificationTasks)

        doFirst {
            if (disableVerificationTasks.get()) {
                logger.warn("Task $path is disabled because `kotlin.build.disable.verification.tasks` is true")
                throw StopExecutionException("Verification tasks are disabled.")
            }
        }
    }
}
