/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("WasmIdePlatformUtil")
@file:Suppress("DEPRECATION_ERROR", "DeprecatedCallableAddReplaceWith")

package org.jetbrains.kotlin.platform.impl

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.platform.*
import org.jetbrains.kotlin.platform.isWasm
import org.jetbrains.kotlin.platform.wasm.*

abstract class WasmIdePlatformKind : IdePlatformKind() {
    override fun platformByCompilerArguments(arguments: CommonCompilerArguments): TargetPlatform? {
        return if (arguments is K2JSCompilerArguments && arguments.wasm) {
            val wasmTarget = arguments.wasmTarget?.let { WasmTarget.fromName(it) }
            wasmTarget?.let {
                WasmPlatforms.wasmPlatformByTargetVersion(it)
            }
        } else null
    }

    override fun createArguments(): CommonCompilerArguments {
        return K2JSCompilerArguments()
    }

    override val argumentsClass get() = K2JSCompilerArguments::class.java

    @Deprecated(
        message = "IdePlatform is deprecated and will be removed soon, please, migrate to org.jetbrains.kotlin.platform.TargetPlatform",
        level = DeprecationLevel.ERROR
    )
    object Platform : IdePlatform<WasmIdePlatformKind, K2JSCompilerArguments>() {
        override val kind get() = WasmJsIdePlatformKind
        override val version get() = TargetPlatformVersion.NoVersion
        override fun createArguments(init: K2JSCompilerArguments.() -> Unit) = K2JSCompilerArguments().apply(init)
    }
}

object WasmJsIdePlatformKind : WasmIdePlatformKind() {
    override fun supportsTargetPlatform(platform: TargetPlatform): Boolean =
        platform.isWasmJs() || (!platform.isWasmWasi() && platform.isWasm())

    override val defaultPlatform get() = WasmPlatforms.wasmJs

    @Deprecated(
        message = "IdePlatform is deprecated and will be removed soon, please, migrate to org.jetbrains.kotlin.platform.TargetPlatform",
        level = DeprecationLevel.ERROR
    )
    override fun getDefaultPlatform(): IdePlatform<*, *> = WasmIdePlatformKind.Platform

    override fun createArguments(): CommonCompilerArguments {
        return K2JSCompilerArguments()
    }

    override val argumentsClass get() = K2JSCompilerArguments::class.java

    override val name get() = "WebAssembly JS"
}

object WasmWasiIdePlatformKind : WasmIdePlatformKind() {
    override fun supportsTargetPlatform(platform: TargetPlatform): Boolean = platform.isWasmWasi()

    override val defaultPlatform get() = WasmPlatforms.wasmWasi

    @Deprecated(
        message = "IdePlatform is deprecated and will be removed soon, please, migrate to org.jetbrains.kotlin.platform.TargetPlatform",
        level = DeprecationLevel.ERROR
    )
    override fun getDefaultPlatform(): IdePlatform<*, *> = WasmIdePlatformKind.Platform

    override fun createArguments(): CommonCompilerArguments {
        return K2JSCompilerArguments()
    }

    override val argumentsClass get() = K2JSCompilerArguments::class.java

    override val name get() = "WebAssembly WASI"
}

val IdePlatformKind?.isWasmJs
    get() = this is WasmJsIdePlatformKind
