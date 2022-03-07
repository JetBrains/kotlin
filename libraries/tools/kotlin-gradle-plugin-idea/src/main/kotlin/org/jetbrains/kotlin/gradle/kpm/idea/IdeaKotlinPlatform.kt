/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package org.jetbrains.kotlin.gradle.kpm.idea

import java.io.Serializable

sealed interface IdeaKotlinPlatform : Serializable {
    val platformType: String
    val platformDetails: IdeaKotlinPlatformDetails?

    companion object {
        val unknown: IdeaKotlinPlatform = IdeaKotlinPlatformImpl("unknown", null)

        const val wasmPlatformType = "wasm"
        const val nativePlatformType = "native"
        const val jvmPlatformType = "jvm"
        const val jsPlatformType = "js"
    }
}

sealed interface IdeaKotlinPlatformDetails : Serializable

sealed interface IdeaKotlinWasmPlatformDetails : IdeaKotlinPlatformDetails

sealed interface IdeaKotlinNativePlatformDetails : IdeaKotlinPlatformDetails {
    val konanTarget: String
}

sealed interface IdeaKotlinJvmPlatformDetails : IdeaKotlinPlatformDetails {
    val jvmTarget: String
}

sealed interface IdeaKotlinJsPlatformDetails : IdeaKotlinPlatformDetails {
    val isIr: Boolean
}

@InternalKotlinGradlePluginApi
fun IdeaKotlinPlatform.Companion.wasm(): IdeaKotlinPlatform {
    return IdeaKotlinPlatformImpl(wasmPlatformType, IdeaKotlinWasmPlatformDetailsImpl)
}

@InternalKotlinGradlePluginApi
fun IdeaKotlinPlatform.Companion.native(konanTarget: String): IdeaKotlinPlatform {
    return IdeaKotlinPlatformImpl(nativePlatformType, IdeaKotlinNativePlatformDetailsImpl(konanTarget))
}

@InternalKotlinGradlePluginApi
fun IdeaKotlinPlatform.Companion.jvm(jvmTarget: String): IdeaKotlinPlatform {
    return IdeaKotlinPlatformImpl(jvmPlatformType, IdeaKotlinJvmPlatformDetailsImpl(jvmTarget))
}

@InternalKotlinGradlePluginApi
fun IdeaKotlinPlatform.Companion.js(isIr: Boolean): IdeaKotlinPlatform {
    return IdeaKotlinPlatformImpl(jsPlatformType, IdeaKotlinJsPlatformDetailsImpl(isIr))
}

val IdeaKotlinPlatform.isWasm get() = platformType == IdeaKotlinPlatform.wasmPlatformType
val IdeaKotlinPlatform.isNative get() = platformType == IdeaKotlinPlatform.nativePlatformType
val IdeaKotlinPlatform.isJvm get() = platformType == IdeaKotlinPlatform.jvmPlatformType
val IdeaKotlinPlatform.isJs get() = platformType == IdeaKotlinPlatform.jsPlatformType

val IdeaKotlinPlatform.nativeOrNull get() = (platformDetails as? IdeaKotlinNativePlatformDetails)
val IdeaKotlinPlatform.jvmOrNull get() = (platformDetails as? IdeaKotlinJvmPlatformDetails)
val IdeaKotlinPlatform.jsOrNull get() = (platformDetails as? IdeaKotlinJsPlatformDetails)

private data class IdeaKotlinPlatformImpl(
    override val platformType: String, override val platformDetails: IdeaKotlinPlatformDetails?
) : IdeaKotlinPlatform {

    companion object {
        const val serialVersionUID = 0L
    }
}

private object IdeaKotlinWasmPlatformDetailsImpl : IdeaKotlinWasmPlatformDetails {
    private const val serialVersionUID = 0L
}

private data class IdeaKotlinJvmPlatformDetailsImpl(override val jvmTarget: String) : IdeaKotlinJvmPlatformDetails {
    private companion object {
        const val serialVersionUID = 0L
    }
}

private data class IdeaKotlinNativePlatformDetailsImpl(override val konanTarget: String) : IdeaKotlinNativePlatformDetails {
    private companion object {
        const val serialVersionUID = 0L
    }
}

private data class IdeaKotlinJsPlatformDetailsImpl(override val isIr: Boolean) : IdeaKotlinJsPlatformDetails {
    private companion object {
        const val serialVersionUID = 0L
    }
}
