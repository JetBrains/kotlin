/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package org.jetbrains.kotlin.gradle.kpm.idea

import java.io.Serializable

sealed interface IdeaKpmPlatform : Serializable {
    val platformType: String
    val platformDetails: IdeaKpmPlatformDetails?

    companion object {
        val unknown: IdeaKpmPlatform = IdeaKpmPlatformImpl("unknown", null)

        const val wasmPlatformType = "wasm"
        const val nativePlatformType = "native"
        const val jvmPlatformType = "jvm"
        const val jsPlatformType = "js"
    }
}

sealed interface IdeaKpmPlatformDetails : Serializable

sealed interface IdeaKpmWasmPlatformDetails : IdeaKpmPlatformDetails

sealed interface IdeaKpmNativePlatformDetails : IdeaKpmPlatformDetails {
    val konanTarget: String
}

sealed interface IdeaKpmJvmPlatformDetails : IdeaKpmPlatformDetails {
    val jvmTarget: String
}

sealed interface IdeaKpmJsPlatformDetails : IdeaKpmPlatformDetails {
    val isIr: Boolean
}

@InternalKotlinGradlePluginApi
fun IdeaKpmPlatform.Companion.wasm(): IdeaKpmPlatform {
    return IdeaKpmPlatformImpl(wasmPlatformType, IdeaKpmWasmPlatformDetailsImpl)
}

@InternalKotlinGradlePluginApi
fun IdeaKpmPlatform.Companion.native(konanTarget: String): IdeaKpmPlatform {
    return IdeaKpmPlatformImpl(nativePlatformType, IdeaKpmNativePlatformDetailsImpl(konanTarget))
}

@InternalKotlinGradlePluginApi
fun IdeaKpmPlatform.Companion.jvm(jvmTarget: String): IdeaKpmPlatform {
    return IdeaKpmPlatformImpl(jvmPlatformType, IdeaKpmJvmPlatformDetailsImpl(jvmTarget))
}

@InternalKotlinGradlePluginApi
fun IdeaKpmPlatform.Companion.js(isIr: Boolean): IdeaKpmPlatform {
    return IdeaKpmPlatformImpl(jsPlatformType, IdeaKpmJsPlatformDetailsImpl(isIr))
}

val IdeaKpmPlatform.isWasm get() = platformType == IdeaKpmPlatform.wasmPlatformType
val IdeaKpmPlatform.isNative get() = platformType == IdeaKpmPlatform.nativePlatformType
val IdeaKpmPlatform.isJvm get() = platformType == IdeaKpmPlatform.jvmPlatformType
val IdeaKpmPlatform.isJs get() = platformType == IdeaKpmPlatform.jsPlatformType

val IdeaKpmPlatform.nativeOrNull get() = (platformDetails as? IdeaKpmNativePlatformDetails)
val IdeaKpmPlatform.jvmOrNull get() = (platformDetails as? IdeaKpmJvmPlatformDetails)
val IdeaKpmPlatform.jsOrNull get() = (platformDetails as? IdeaKpmJsPlatformDetails)

private data class IdeaKpmPlatformImpl(
    override val platformType: String, override val platformDetails: IdeaKpmPlatformDetails?
) : IdeaKpmPlatform {

    companion object {
        const val serialVersionUID = 0L
    }
}

private object IdeaKpmWasmPlatformDetailsImpl : IdeaKpmWasmPlatformDetails {
    private const val serialVersionUID = 0L
}

private data class IdeaKpmJvmPlatformDetailsImpl(override val jvmTarget: String) : IdeaKpmJvmPlatformDetails {
    private companion object {
        const val serialVersionUID = 0L
    }
}

private data class IdeaKpmNativePlatformDetailsImpl(override val konanTarget: String) : IdeaKpmNativePlatformDetails {
    private companion object {
        const val serialVersionUID = 0L
    }
}

private data class IdeaKpmJsPlatformDetailsImpl(override val isIr: Boolean) : IdeaKpmJsPlatformDetails {
    private companion object {
        const val serialVersionUID = 0L
    }
}
