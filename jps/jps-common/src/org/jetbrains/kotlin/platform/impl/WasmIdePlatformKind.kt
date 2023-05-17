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
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms

object WasmIdePlatformKind : IdePlatformKind() {
    override fun supportsTargetPlatform(platform: TargetPlatform): Boolean = platform.isWasm()

    override fun platformByCompilerArguments(arguments: CommonCompilerArguments): TargetPlatform? {
        return if (arguments is K2JSCompilerArguments && arguments.wasm)
            WasmPlatforms.Default
        else
            null
    }

    val platforms get() = listOf(WasmPlatforms.Default)
    override val defaultPlatform get() = WasmPlatforms.Default

    @Deprecated(
        message = "IdePlatform is deprecated and will be removed soon, please, migrate to org.jetbrains.kotlin.platform.TargetPlatform",
        level = DeprecationLevel.ERROR
    )
    override fun getDefaultPlatform(): IdePlatform<*, *> = WasmIdePlatformKind.Platform

    override fun createArguments(): CommonCompilerArguments {
        return K2JSCompilerArguments()
    }

    override val argumentsClass get() = K2JSCompilerArguments::class.java

    override val name get() = "WebAssembly"

    @Deprecated(
        message = "IdePlatform is deprecated and will be removed soon, please, migrate to org.jetbrains.kotlin.platform.TargetPlatform",
        level = DeprecationLevel.ERROR
    )
    object Platform : IdePlatform<WasmIdePlatformKind, K2JSCompilerArguments>() {
        override val kind get() = WasmIdePlatformKind
        override val version get() = TargetPlatformVersion.NoVersion
        override fun createArguments(init: K2JSCompilerArguments.() -> Unit) = K2JSCompilerArguments().apply(init)
    }
}

val IdePlatformKind?.isWasm
    get() = this is WasmIdePlatformKind
