/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType

private const val TEST_DRY_RUN_KEY = "kotlin.build.test.dry.run"
private const val TEST_DRY_RUN_ENV_KEY = "KOTLIN_BUILD_TEST_DRY_RUN"

internal fun Project.configureTestDryRun() {
    val testDryRun = providers.gradleProperty(TEST_DRY_RUN_KEY)
        .orElse(providers.environmentVariable(TEST_DRY_RUN_ENV_KEY))
        .map(String::toBooleanStrict)
        .orElse(false)

    tasks.withType<Test>().configureEach {
        dryRun.value(testDryRun)
    }
}
