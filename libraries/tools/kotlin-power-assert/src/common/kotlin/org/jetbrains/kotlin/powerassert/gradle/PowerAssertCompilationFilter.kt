/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert.gradle

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

/**
 * Filters [KotlinCompilation]s to be transformed by the Power-Assert compiler plugin.
 */
fun interface PowerAssertCompilationFilter {
    /**
     * Checks if the given [compilation] should be transformed by the compiler plugin.
     */
    fun check(compilation: KotlinCompilation<*>): Boolean

    companion object {
        /**
         * All Kotlin compilations should be transformed by default.
         */
        val ALL: PowerAssertCompilationFilter = PowerAssertCompilationFilter {
            true
        }

        /**
         * Only test Kotlin compilation should be transformed by default.
         */
        val TESTS: PowerAssertCompilationFilter = PowerAssertCompilationFilter {
            // TODO(KT-68534): Better handle Android test variant compilations.
            it.name == KotlinCompilation.TEST_COMPILATION_NAME
        }
    }
}
