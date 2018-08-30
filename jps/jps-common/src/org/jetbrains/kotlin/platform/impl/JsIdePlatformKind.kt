/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("JsIdePlatformUtil")
package org.jetbrains.kotlin.platform.impl

import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.config.TargetPlatformVersion
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.platform.IdePlatform
import org.jetbrains.kotlin.platform.IdePlatformKind

object JsIdePlatformKind : IdePlatformKind<JsIdePlatformKind>() {
    override val compilerPlatform get() = JsPlatform

    override val platforms get() = listOf(Platform)
    override val defaultPlatform get() = Platform

    override val argumentsClass get() = K2JSCompilerArguments::class.java

    override val name get() = "JavaScript"

    object Platform : IdePlatform<JsIdePlatformKind, K2JSCompilerArguments>() {
        override val kind get() = JsIdePlatformKind
        override val version get() = TargetPlatformVersion.NoVersion
        override fun createArguments(init: K2JSCompilerArguments.() -> Unit) = K2JSCompilerArguments().apply(init)
    }
}

val IdePlatformKind<*>?.isJavaScript
    get() = this is JsIdePlatformKind

val IdePlatform<*, *>?.isJavaScript
    get() = this is JsIdePlatformKind.Platform
