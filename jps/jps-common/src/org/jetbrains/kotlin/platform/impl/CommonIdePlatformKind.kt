/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("CommonIdePlatformUtil")
@file:Suppress("DEPRECATION_ERROR", "DeprecatedCallableAddReplaceWith")

package org.jetbrains.kotlin.platform.impl

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.platform.*

object CommonIdePlatformKind : IdePlatformKind() {
    override fun supportsTargetPlatform(platform: TargetPlatform) = platform.isCommon()

    override fun platformByCompilerArguments(arguments: CommonCompilerArguments): TargetPlatform? {
        return if (arguments is K2MetadataCompilerArguments)
            CommonPlatforms.defaultCommonPlatform
        else
            null
    }

    @Deprecated(
        message = "IdePlatform is deprecated and will be removed soon, please, migrate to org.jetbrains.kotlin.platform.TargetPlatform",
        level = DeprecationLevel.ERROR
    )
    override fun getDefaultPlatform(): IdePlatform<*, *> = Platform

    override fun createArguments(): CommonCompilerArguments {
        return K2MetadataCompilerArguments() // TODO(dsavvinov): review that, as now MPP !== K2Metadata
    }

    override val defaultPlatform get() = CommonPlatforms.defaultCommonPlatform

    override val argumentsClass get() = K2MetadataCompilerArguments::class.java

    override val name get() = "Common (experimental)"

    @Deprecated(
        message = "IdePlatform is deprecated and will be removed soon, please, migrate to org.jetbrains.kotlin.platform.TargetPlatform",
        level = DeprecationLevel.ERROR
    )
    object Platform : IdePlatform<CommonIdePlatformKind, CommonCompilerArguments>() {
        override val kind get() = CommonIdePlatformKind
        override val version get() = TargetPlatformVersion.NoVersion
        override fun createArguments(init: CommonCompilerArguments.() -> Unit) = K2MetadataCompilerArguments().apply(init)
    }
}

val IdePlatformKind?.isCommon
    get() = this is CommonIdePlatformKind

@Deprecated(
    message = "IdePlatform is deprecated and will be removed soon, please, migrate to org.jetbrains.kotlin.platform.TargetPlatform",
    level = DeprecationLevel.ERROR
)
val IdePlatform<*, *>.isCommon: Boolean
    get() = this is CommonIdePlatformKind.Platform