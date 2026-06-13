/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dsl

import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl

/**
 * Live-service that will be used at task execution time
 * to spawn a dev web server serving prepared HTML page
 * with Kotlin JS tests and runner to run them.
 *
 */
@ExperimentalJsTestDsl
interface KotlinJsTestDevServerService {
    /**
     * Provides safe access to the dev server URL.
     * When invoked, a dev-server will be prepared in place and, when ready, [code] will be invoked.
     */
    fun <T> use(code: (baseUrl: String) -> T): T
}
