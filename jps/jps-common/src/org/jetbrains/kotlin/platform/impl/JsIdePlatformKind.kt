/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("JsIdePlatformUtil")
@file:Suppress("DEPRECATION_ERROR", "DeprecatedCallableAddReplaceWith")

package org.jetbrains.kotlin.platform.impl

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.platform.IdePlatform
import org.jetbrains.kotlin.platform.IdePlatformKind
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.TargetPlatformVersion
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.js.isJs

object JsIdePlatformKind : IdePlatformKind() {
    override fun supportsTargetPlatform(platform: TargetPlatform): Boolean = platform.isJs()

    override fun platformByCompilerArguments(arguments: CommonCompilerArguments): TargetPlatform? {
        return if (arguments is K2JSCompilerArguments)
            JsPlatforms.defaultJsPlatform
        else
            null
    }

    val platforms get() = listOf(JsPlatforms.defaultJsPlatform)
    override val defaultPlatform get() = JsPlatforms.defaultJsPlatform

    @Deprecated(
        message = "IdePlatform is deprecated and will be removed soon, please, migrate to org.jetbrains.kotlin.platform.TargetPlatform",
        level = DeprecationLevel.ERROR
    )
    override fun getDefaultPlatform(): IdePlatform<*, *> = Platform

    override fun createArguments(): CommonCompilerArguments {
        return K2JSCompilerArguments()
    }

    override val argumentsClass get() = K2JSCompilerArguments::class.java

    override val name get() = "JavaScript"

    @Deprecated(
        message = "IdePlatform is deprecated and will be removed soon, please, migrate to org.jetbrains.kotlin.platform.TargetPlatform",
        level = DeprecationLevel.ERROR
    )
    object Platform : IdePlatform<JsIdePlatformKind, K2JSCompilerArguments>() {
        override val kind get() = JsIdePlatformKind
        override val version get() = TargetPlatformVersion.NoVersion
        override fun createArguments(init: K2JSCompilerArguments.() -> Unit) = K2JSCompilerArguments().apply(init)
    }
}

val IdePlatformKind?.isJavaScript
    get() = this is JsIdePlatformKind

@Deprecated(
    message = "IdePlatform is deprecated and will be removed soon, please, migrate to org.jetbrains.kotlin.platform.TargetPlatform",
    level = DeprecationLevel.ERROR
)
val IdePlatform<*, *>?.isJavaScript
    get() = this is JsIdePlatformKind.Platform
