/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.provider.ListProperty

/**
 * The `testDataManager { ... }` extension of the `test-data-manager` plugin.
 */
abstract class TestDataManagerExtension {
    /**
     * Paths of projects whose `test` and test data manager tasks must run before this project's ones,
     * e.g., `:analysis:analysis-api-fir` to run golden tests first.
     *
     * Declared here rather than as `test.mustRunAfter`: reading task dependencies is prohibited with isolated
     * projects, so the manager tasks couldn't inherit it.
     */
    abstract val mustRunAfterProjects: ListProperty<String>

    /**
     * Packages of the managed tests of this project, `org.jetbrains.kotlin` if empty. The test data manager discovers tests only in
     * these packages and their subpackages, so it needs them for tests in other packages, and narrower ones make the discovery faster.
     */
    abstract val testPackages: ListProperty<String>
}
