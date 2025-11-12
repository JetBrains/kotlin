/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dsl

import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode.DEVELOPMENT
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode.PRODUCTION

/**
 * The mode that describes how WasmJS and JS
 * [binaries][org.jetbrains.kotlin.gradle.targets.js.ir.JsBinary]
 * are built.
 *
 * All JS and WasmJS binaries will have a [PRODUCTION] and [DEVELOPMENT] variants.
 *
 * The mode affects how KGP builds the binary.
 *
 * #### Kotlin compilation.
 * - For [PRODUCTION], enable code minimisation, Dead Code Elimination.
 * - For [DEVELOPMENT], enable incremental compilation, and include debugging information.
 *
 * #### Bundling
 *
 * For example, sets Webpack [mode](https://webpack.js.org/configuration/mode/).
 *
 * @see org.jetbrains.kotlin.gradle.targets.js.ir.JsBinary.mode
 */
enum class KotlinJsBinaryMode {
    /**
     * Production mode.
     *
     * When KGP builds a binary with this mode, it enables options
     * to optimise for size and performance.
     *
     * The build times may increase.
     */
    PRODUCTION,

    /**
     * Development mode.
     *
     * When KGP builds a binary with this mode, it enables options
     * to optimise for improving local development experience.
     *
     * Use for local development only.
     */
    DEVELOPMENT
}
