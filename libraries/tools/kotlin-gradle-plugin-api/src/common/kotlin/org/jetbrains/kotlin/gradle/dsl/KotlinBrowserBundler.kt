/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

/**
 * Enumeration representing different bundlers available for Kotlin/Wasm and Kotlin/JS browser applications.
 *
 * This enum defines the options for bundling JavaScript files when targeting the browser platform.
 * It determines how the compiled Kotlin code will be packaged and optimized for web deployment.
 */
@ExperimentalWasmDsl
enum class KotlinBrowserBundler {
    /**
     * Use Webpack for bundling.
     *
     * This option enables the use of Webpack as the bundler for the compiled Kotlin/JS and Kotlin/Wasm application.
     */
    WEBPACK,
    
    /**
     * Do not use any bundler (no bundling).
     *
     * The output will consist of individual JavaScript and Wasm files without any bundling process.
     */
    NONE;
}
