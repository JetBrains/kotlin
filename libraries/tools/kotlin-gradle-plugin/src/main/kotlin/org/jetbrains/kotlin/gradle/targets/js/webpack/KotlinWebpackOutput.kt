/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.webpack

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

data class KotlinWebpackOutput(
    @Input
    @Optional
    var library: String?,
    @Input
    @Optional
    var libraryTarget: String?,
    @Input
    var globalObject: String = "window"
) {
    object Target {
        const val VAR = "var"
        const val ASSIGN = "assign"
        const val THIS = "this"
        const val WINDOW = "window"
        const val SELF = "self"
        const val GLOBAL = "global"
        const val COMMONJS = "commonjs"
        const val COMMONJS2 = "commonjs2"
        const val COMMONJS_MODULE = "commonjs-module"
        const val AMD = "amd"
        const val AMD_REQUIRE = "amd-require"
        const val UMD = "umd"
        const val UMD2 = "umd2"
        const val JSONP = "jsonp"
        const val SYSTEM = "system"
    }
}