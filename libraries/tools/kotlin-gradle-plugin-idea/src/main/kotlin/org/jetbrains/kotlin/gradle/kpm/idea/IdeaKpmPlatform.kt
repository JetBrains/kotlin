/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.tooling.core.Extras
import org.jetbrains.kotlin.tooling.core.emptyExtras
import java.io.Serializable

sealed interface IdeaKpmPlatform : Serializable {
    val extras: Extras
}

sealed interface IdeaKpmJvmPlatform : IdeaKpmPlatform {
    val jvmTarget: String
}

sealed interface IdeaKpmNativePlatform : IdeaKpmPlatform {
    val konanTarget: String
}

sealed interface IdeaKpmJsPlatform : IdeaKpmPlatform {
    val isIr: Boolean
}

sealed interface IdeaKpmWasmPlatform : IdeaKpmPlatform

sealed interface IdeaKpmUnknownPlatform : IdeaKpmPlatform

val IdeaKpmPlatform.isWasm get() = this is IdeaKpmWasmPlatform
val IdeaKpmPlatform.isNative get() = this is IdeaKpmNativePlatform
val IdeaKpmPlatform.isJvm get() = this is IdeaKpmJvmPlatform
val IdeaKpmPlatform.isJs get() = this is IdeaKpmJsPlatform
val IdeaKpmPlatform.isUnknown get() = this is IdeaKpmUnknownPlatform

@InternalKotlinGradlePluginApi
data class IdeaKpmJvmPlatformImpl(
    override val jvmTarget: String,
    override val extras: Extras = emptyExtras()
) : IdeaKpmJvmPlatform {
    internal companion object {
        const val serialVersionUID = 0L
    }
}

@InternalKotlinGradlePluginApi
data class IdeaKpmNativePlatformImpl(
    override val konanTarget: String,
    override val extras: Extras = emptyExtras()
) : IdeaKpmNativePlatform {
    internal companion object {
        const val serialVersionUID = 0L
    }
}

@InternalKotlinGradlePluginApi
data class IdeaKpmJsPlatformImpl(
    override val isIr: Boolean,
    override val extras: Extras = emptyExtras()
) : IdeaKpmJsPlatform {
    internal companion object {
        const val serialVersionUID = 0L
    }
}

@InternalKotlinGradlePluginApi
data class IdeaKpmWasmPlatformImpl(
    override val extras: Extras = emptyExtras()
) : IdeaKpmWasmPlatform {
    internal companion object {
        const val serialVersionUID = 0L
    }
}

@InternalKotlinGradlePluginApi
data class IdeaKpmUnknownPlatformImpl(
    override val extras: Extras = emptyExtras()
) : IdeaKpmUnknownPlatform {
    internal companion object {
        const val serialVersionUID = 0L
    }
}
