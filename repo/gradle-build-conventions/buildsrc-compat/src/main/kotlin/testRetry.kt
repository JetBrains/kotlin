/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import com.gradle.enterprise.gradleplugin.testretry.retry
import org.gradle.kotlin.dsl.withType

fun Project.configureTestRetriesForTestTasks() {
    val testRetryMaxRetries = findProperty("kotlin.build.testRetry.maxRetries")
        ?.toString()?.toInt()
        ?: (if (kotlinBuildProperties.isTeamcityBuild) 3 else 0)

    tasks.withType<Test>().configureEach {
        retry {
            maxRetries.set(testRetryMaxRetries)
            maxFailures.set(20)
            failOnPassedAfterRetry.set(false)
        }
    }
}
