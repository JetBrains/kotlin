/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web

/** A platform that runs in a JavaScript host, a browser or Node.js, and shares its tooling, such as npm. */
internal enum class KotlinWebPlatformType(
    /** Used in the names of tasks, configurations and directories. */
    val platformName: String,
) {
    JS("js"),
    WasmJS("wasmJs"),
}
