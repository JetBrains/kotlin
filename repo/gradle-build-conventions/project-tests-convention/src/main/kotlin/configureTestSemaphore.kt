/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.kotlin.dsl.registerIfAbsent
import org.gradle.kotlin.dsl.withType


internal abstract class TestSemaphoreService : BuildService<BuildServiceParameters.None>

internal fun Project.configureTestSemaphore() {
    val testSemaphore = gradle.sharedServices.registerIfAbsent(
        "project.tests.convention:testSemaphore",
        TestSemaphoreService::class
    ) {
        maxParallelUsages.set(1)
    }

    tasks.withType<AbstractTestTask>().configureEach {
        if (project.kotlinBuildProperties.limitTestTasksConcurrency) {
            usesService(testSemaphore)
        }
    }
}
