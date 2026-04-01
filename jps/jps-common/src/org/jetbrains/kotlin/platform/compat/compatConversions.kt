/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION_ERROR", "TYPEALIAS_EXPANSION_DEPRECATION_ERROR")

package org.jetbrains.kotlin.platform.compat

import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.IdePlatform
import org.jetbrains.kotlin.platform.JsPlatform
import org.jetbrains.kotlin.platform.WasmPlatform
import org.jetbrains.kotlin.platform.impl.*
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JdkPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.konan.NativePlatform
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms

typealias NewPlatform = org.jetbrains.kotlin.platform.TargetPlatform

fun IdePlatform<*, *>.toNewPlatform(): NewPlatform = when (this) {
    is CommonIdePlatformKind.Platform -> CommonPlatforms.defaultCommonPlatform
    is JvmIdePlatformKind.Platform -> JvmPlatforms.jvmPlatformByTargetVersion(this.version)
    is JsIdePlatformKind.Platform -> JsPlatforms.defaultJsPlatform
    is WasmIdePlatformKind.Platform -> WasmPlatforms.wasmJs
    is NativeIdePlatformKind.Platform -> NativePlatforms.unspecifiedNativePlatform
    else -> error("Unknown platform $this")
}

fun NewPlatform.toIdePlatform(): IdePlatform<*, *> = when (val single = singleOrNull()) {
    null -> CommonIdePlatformKind.Platform
    is JdkPlatform -> JvmIdePlatformKind.Platform(single.targetVersion)
    is JvmPlatform -> JvmIdePlatformKind.Platform(JvmTarget.DEFAULT)
    is JsPlatform -> JsIdePlatformKind.Platform
    is WasmPlatform -> WasmIdePlatformKind.Platform
    is NativePlatform -> NativeIdePlatformKind.Platform
    else -> error("Unknown platform $single")
}
