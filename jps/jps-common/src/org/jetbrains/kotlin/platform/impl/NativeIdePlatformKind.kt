/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("NativeIdePlatformUtil")
@file:Suppress("DEPRECATION_ERROR", "DeprecatedCallableAddReplaceWith")

package org.jetbrains.kotlin.platform.impl

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.Freezable
import org.jetbrains.kotlin.cli.common.arguments.copyCommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.platform.IdePlatform
import org.jetbrains.kotlin.platform.IdePlatformKind
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.TargetPlatformVersion
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.platform.konan.isNative

object NativeIdePlatformKind : IdePlatformKind() {
    override fun supportsTargetPlatform(platform: TargetPlatform): Boolean = platform.isNative()

    override fun platformByCompilerArguments(arguments: CommonCompilerArguments): TargetPlatform? {
        return if (arguments is K2NativeCompilerArguments)
            NativePlatforms.unspecifiedNativePlatform
        else
            null
    }

    override fun createArguments(): K2NativeCompilerArguments {
        return K2NativeCompilerArguments()
    }

    override val defaultPlatform: TargetPlatform
        get() = NativePlatforms.unspecifiedNativePlatform

    @Deprecated(
        message = "IdePlatform is deprecated and will be removed soon, please, migrate to org.jetbrains.kotlin.platform.TargetPlatform",
        level = DeprecationLevel.ERROR
    )
    override fun getDefaultPlatform(): IdePlatform<*, *> = Platform

    override val argumentsClass
        get() = K2NativeCompilerArguments::class.java

    override val name
        get() = "Native"

    @Deprecated(
        message = "IdePlatform is deprecated and will be removed soon, please, migrate to org.jetbrains.kotlin.platform.TargetPlatform",
        level = DeprecationLevel.ERROR
    )
    object Platform : IdePlatform<NativeIdePlatformKind, K2NativeCompilerArguments>() {
        override val kind get() = NativeIdePlatformKind
        override val version get() = TargetPlatformVersion.NoVersion
        override fun createArguments(init: K2NativeCompilerArguments.() -> Unit) = K2NativeCompilerArguments().apply(init)
    }
}

// These are fake compiler arguments for Kotlin/Native - only for usage within IDEA plugin:
@Deprecated(
    message = "Use K2NativeCompilerArguments instead",
    level = DeprecationLevel.ERROR
)
class FakeK2NativeCompilerArguments : CommonCompilerArguments() {
    override fun copyOf(): Freezable = copyCommonCompilerArguments(this, FakeK2NativeCompilerArguments())
}

val IdePlatformKind?.isKotlinNative
    get() = this is NativeIdePlatformKind

@Deprecated(
    message = "IdePlatform is deprecated and will be removed soon, please, migrate to org.jetbrains.kotlin.platform.TargetPlatform",
    level = DeprecationLevel.ERROR
)
val IdePlatform<*, *>?.isKotlinNative
    get() = this is NativeIdePlatformKind.Platform
