/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.webpack

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackCssMode.INLINE

data class KotlinWebpackCssSupport(
    @Input
    var enabled: Boolean = false,

    @Nested
    var rules: MutableList<KotlinWebpackCssRule> = mutableListOf(
        KotlinWebpackCssRule()
    )
) {
    @get:Internal
    var mode: String
        get() =
            rules
                .singleOrNull()
                ?.mode ?: singleRuleError()
        set(value) {
            rules.singleOrNull()
                ?.let {
                    it.mode = value
                } ?: singleRuleError()
        }

    private fun singleRuleError(): Nothing {
        throw error("CSS mode shortcut can be applied only with one css rule")
    }
}

data class KotlinWebpackCssRule(
    @Input
    var mode: String = INLINE,

    @Input
    var include: MutableList<String> = mutableListOf(),

    @Input
    var exclude: MutableList<String> = mutableListOf()
)

object KotlinWebpackCssMode {
    const val EXTRACT = "extract"
    const val INLINE = "inline"
    const val IMPORT = "import"
}