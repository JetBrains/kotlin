/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType
import org.gradle.process.ProcessForkOptions
import kotlin.text.toBoolean

internal fun Project.configureTestCacheDisabling() {
    tasks.withType<AbstractTestTask>().configureEach {
        // `kotlin.build.cache.tests.disabled` property is used for master builds to always run the tests
        val testCacheDisabled = providers.gradleProperty("kotlin.build.cache.tests.disabled").orElse("false").get().toBoolean()

        val isNightlyBuild: Boolean = providers.gradleProperty("nightly").map { it.toBooleanStrict() }
            .orElse(providers.environmentVariable("NIGHTLY").map { it.toBooleanStrict() })
            .orElse(false)
            .get()

        val doNotUseCaches = testCacheDisabled || isNightlyBuild

        // We don't actually disable the tests, just upToDateWhen, so we still push to the BuildCache
        outputs.upToDateWhen { !doNotUseCaches }

        if (this is Test || this is ProcessForkOptions) {
            val rootDir = project.rootDir
            outputs.doNotCacheIf("Caching tests is disabled because `workingDir` is set to `rootDir`") { workingDir == rootDir }
        }
    }
}
