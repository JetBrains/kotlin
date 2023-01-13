/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto.kpm

import org.jetbrains.kotlin.gradle.idea.kpm.*
import org.jetbrains.kotlin.gradle.idea.proto.Extras
import org.jetbrains.kotlin.gradle.idea.proto.IdeaExtrasProto
import org.jetbrains.kotlin.gradle.idea.proto.generated.kpm.*
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinSerializationContext

internal fun IdeaKotlinSerializationContext.IdeaKpmPlatformProto(platform: IdeaKpmPlatform): IdeaKpmPlatformProto {
    return ideaKpmPlatformProto {
        when (platform) {
            is IdeaKpmJsPlatformImpl -> js = IdeaKpmJsPlatformProto(platform)
            is IdeaKpmJvmPlatformImpl -> jvm = IdeaKpmJvmPlatformProto(platform)
            is IdeaKpmNativePlatformImpl -> native = IdeaKpmNativePlatformProto(platform)
            is IdeaKpmUnknownPlatformImpl -> unknown = IdeaKpmUnknownPlatformProto(platform)
            is IdeaKpmWasmPlatformImpl -> wasm = IdeaKpmWasmPlatformProto(platform)
        }
    }
}

internal fun IdeaKotlinSerializationContext.IdeaKpmPlatform(proto: IdeaKpmPlatformProto): IdeaKpmPlatform {
    return when (proto.platformCase) {
        IdeaKpmPlatformProto.PlatformCase.JVM -> IdeaKpmJvmPlatform(proto.jvm)
        IdeaKpmPlatformProto.PlatformCase.NATIVE -> IdeaKpmNativePlatform(proto.native)
        IdeaKpmPlatformProto.PlatformCase.JS -> IdeaKpmJsPlatform(proto.js)
        IdeaKpmPlatformProto.PlatformCase.WASM -> IdeaKpmWasmPlatform(proto.wasm)
        IdeaKpmPlatformProto.PlatformCase.UNKNOWN -> IdeaKpmUnknownPlatform(proto.unknown)
        IdeaKpmPlatformProto.PlatformCase.PLATFORM_NOT_SET, null -> IdeaKpmUnknownPlatformImpl()
    }
}

/* Jvm */

internal fun IdeaKotlinSerializationContext.IdeaKpmJvmPlatformProto(platform: IdeaKpmJvmPlatform): IdeaKpmJvmPlatformProto {
    return ideaKpmJvmPlatformProto {
        if (platform.extras.isNotEmpty()) extras = IdeaExtrasProto(platform.extras)
        jvmTarget = platform.jvmTarget
    }
}

internal fun IdeaKotlinSerializationContext.IdeaKpmJvmPlatform(proto: IdeaKpmJvmPlatformProto): IdeaKpmJvmPlatform {
    return IdeaKpmJvmPlatformImpl(
        jvmTarget = proto.jvmTarget,
        extras = Extras(proto.extras)
    )
}

internal fun IdeaKotlinSerializationContext.IdeaKpmJvmPlatform(data: ByteArray): IdeaKpmJvmPlatform {
    return IdeaKpmJvmPlatform(IdeaKpmJvmPlatformProto.parseFrom(data))
}

internal fun IdeaKpmJvmPlatform.toByteArray(context: IdeaKotlinSerializationContext): ByteArray {
    return context.IdeaKpmJvmPlatformProto(this).toByteArray()
}

/* Native */

internal fun IdeaKotlinSerializationContext.IdeaKpmNativePlatformProto(platform: IdeaKpmNativePlatform): IdeaKpmNativePlatformProto {
    return ideaKpmNativePlatformProto {
        if (platform.extras.isNotEmpty()) extras = IdeaExtrasProto(platform.extras)
        konanTarget = platform.konanTarget
    }
}

internal fun IdeaKotlinSerializationContext.IdeaKpmNativePlatform(proto: IdeaKpmNativePlatformProto): IdeaKpmNativePlatform {
    return IdeaKpmNativePlatformImpl(
        konanTarget = proto.konanTarget,
        extras = Extras(proto.extras)
    )
}

internal fun IdeaKotlinSerializationContext.IdeaKpmNativePlatform(data: ByteArray): IdeaKpmNativePlatform {
    return IdeaKpmNativePlatform(IdeaKpmNativePlatformProto.parseFrom(data))
}

internal fun IdeaKpmNativePlatform.toByteArray(context: IdeaKotlinSerializationContext): ByteArray {
    return context.IdeaKpmNativePlatformProto(this).toByteArray()
}

/* Js */

internal fun IdeaKotlinSerializationContext.IdeaKpmJsPlatformProto(platform: IdeaKpmJsPlatform): IdeaKpmJsPlatformProto {
    return ideaKpmJsPlatformProto {
        if (platform.extras.isNotEmpty()) extras = IdeaExtrasProto(platform.extras)
        isIr = platform.isIr
    }
}

internal fun IdeaKotlinSerializationContext.IdeaKpmJsPlatform(proto: IdeaKpmJsPlatformProto): IdeaKpmJsPlatform {
    return IdeaKpmJsPlatformImpl(
        isIr = proto.isIr,
        extras = Extras(proto.extras)
    )
}

internal fun IdeaKotlinSerializationContext.IdeaKpmJsPlatform(data: ByteArray): IdeaKpmJsPlatform {
    return IdeaKpmJsPlatform(IdeaKpmJsPlatformProto.parseFrom(data))
}

internal fun IdeaKpmJsPlatform.toByteArray(context: IdeaKotlinSerializationContext): ByteArray {
    return context.IdeaKpmJsPlatformProto(this).toByteArray()
}

/* Wasm */

internal fun IdeaKotlinSerializationContext.IdeaKpmWasmPlatformProto(platform: IdeaKpmWasmPlatform): IdeaKpmWasmPlatformProto {
    return ideaKpmWasmPlatformProto {
        if (platform.extras.isNotEmpty()) extras = IdeaExtrasProto(platform.extras)
    }
}

internal fun IdeaKotlinSerializationContext.IdeaKpmWasmPlatform(proto: IdeaKpmWasmPlatformProto): IdeaKpmWasmPlatform {
    return IdeaKpmWasmPlatformImpl(
        extras = Extras(proto.extras)
    )
}

internal fun IdeaKotlinSerializationContext.IdeaKpmWasmPlatform(data: ByteArray): IdeaKpmWasmPlatform {
    return IdeaKpmWasmPlatform(IdeaKpmWasmPlatformProto.parseFrom(data))
}

internal fun IdeaKpmWasmPlatform.toByteArray(context: IdeaKotlinSerializationContext): ByteArray {
    return context.IdeaKpmWasmPlatformProto(this).toByteArray()
}

/* Unknown */

internal fun IdeaKotlinSerializationContext.IdeaKpmUnknownPlatformProto(platform: IdeaKpmUnknownPlatform): IdeaKpmUnknownPlatformProto {
    return ideaKpmUnknownPlatformProto {
        if (platform.extras.isNotEmpty()) extras = IdeaExtrasProto(platform.extras)
    }
}

internal fun IdeaKotlinSerializationContext.IdeaKpmUnknownPlatform(proto: IdeaKpmUnknownPlatformProto): IdeaKpmUnknownPlatform {
    return IdeaKpmUnknownPlatformImpl(
        extras = Extras(proto.extras)
    )
}

internal fun IdeaKotlinSerializationContext.IdeaKpmUnknownPlatform(data: ByteArray): IdeaKpmUnknownPlatform {
    return IdeaKpmUnknownPlatform(IdeaKpmUnknownPlatformProto.parseFrom(data))
}

internal fun IdeaKpmUnknownPlatform.toByteArray(context: IdeaKotlinSerializationContext): ByteArray {
    return context.IdeaKpmUnknownPlatformProto(this).toByteArray()
}
