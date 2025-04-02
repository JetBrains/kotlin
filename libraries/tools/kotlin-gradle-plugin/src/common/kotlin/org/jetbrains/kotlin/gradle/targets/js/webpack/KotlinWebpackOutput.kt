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
    var globalObject: String = "globalThis",
    @Input
    @Optional
    var clean: Boolean? = null,
) {
    @Deprecated("No longer used. Scheduled for removal in Kotlin 2.4.")
    object Target {
        @Deprecated("No longer used. Scheduled for removal in Kotlin 2.4.", ReplaceWith("var"))
        const val VAR = "var"

        @Deprecated("No longer used. Scheduled for removal in Kotlin 2.4.", ReplaceWith("assign"))
        const val ASSIGN = "assign"

        @Deprecated("No longer used. Scheduled for removal in Kotlin 2.4.", ReplaceWith("this"))
        const val THIS = "this"

        @Deprecated("No longer used. Scheduled for removal in Kotlin 2.4.", ReplaceWith("window"))
        const val WINDOW = "window"

        @Deprecated("No longer used. Scheduled for removal in Kotlin 2.4.", ReplaceWith("self"))
        const val SELF = "self"

        @Deprecated("No longer used. Scheduled for removal in Kotlin 2.4.", ReplaceWith("global"))
        const val GLOBAL = "global"

        @Deprecated("No longer used. Scheduled for removal in Kotlin 2.4.", ReplaceWith("commonjs"))
        const val COMMONJS = "commonjs"

        @Deprecated("No longer used. Scheduled for removal in Kotlin 2.4.", ReplaceWith("commonjs2"))
        const val COMMONJS2 = "commonjs2"

        @Deprecated("No longer used. Scheduled for removal in Kotlin 2.4.", ReplaceWith("module"))
        const val COMMONJS_MODULE = "commonjs-module"

        @Deprecated("No longer used. Scheduled for removal in Kotlin 2.4.", ReplaceWith("amd"))
        const val AMD = "amd"

        @Deprecated("No longer used. Scheduled for removal in Kotlin 2.4.", ReplaceWith("require"))
        const val AMD_REQUIRE = "amd-require"

        @Deprecated("No longer used. Scheduled for removal in Kotlin 2.4.", ReplaceWith("umd"))
        const val UMD = "umd"

        @Deprecated("No longer used. Scheduled for removal in Kotlin 2.4.", ReplaceWith("umd2"))
        const val UMD2 = "umd2"

        @Deprecated("No longer used. Scheduled for removal in Kotlin 2.4.", ReplaceWith("jsonp"))
        const val JSONP = "jsonp"

        @Deprecated("No longer used. Scheduled for removal in Kotlin 2.4.", ReplaceWith("system"))
        const val SYSTEM = "system"
    }
}
