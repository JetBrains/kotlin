/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinBrowserBundler
import org.jetbrains.kotlin.gradle.targets.wasm.dsl.KotlinWasmJsBrowserDsl
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalWasmDsl::class)
class WasmBrowserBundlerDslTest {

    @Test
    fun `test browser(NONE) captures NONE`() {
        buildProjectWithMPP {
            kotlin {
                wasmJs {
                    browser(KotlinBrowserBundler.NONE) {
                        assertTrue {
                            bundler.get() == KotlinBrowserBundler.NONE
                        }
                    }
                }
            }
        }.evaluate()
    }

    @Test
    fun `test browser(WEBPACK) captures WEBPACK`() {
        buildProjectWithMPP {
            kotlin {
                wasmJs {
                    browser(KotlinBrowserBundler.WEBPACK) {
                        assertTrue {
                            bundler.get() == KotlinBrowserBundler.WEBPACK
                        }
                    }
                }
            }
        }.evaluate()
    }

    @Test
    fun `test no-argument browser { } overload captures WEBPACK`() {
        buildProjectWithMPP {
            kotlin {
                wasmJs {
                    browser {
                        this as KotlinWasmJsBrowserDsl
                        assertTrue {
                            bundler.get() == KotlinBrowserBundler.WEBPACK
                        }
                    }
                }
            }
        }.evaluate()
    }
}
