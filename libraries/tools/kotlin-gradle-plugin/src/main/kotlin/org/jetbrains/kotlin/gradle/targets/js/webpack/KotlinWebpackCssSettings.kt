/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.webpack

import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackCssMode.INLINE

data class KotlinWebpackCssSettings(
    @Input
    var enabled: Boolean = true,

    @Input
    var mode: String = INLINE
)

object KotlinWebpackCssMode {
    const val EXTRACT = "extract"
    const val INLINE = "inline"
    const val IMPORT = "import"
}