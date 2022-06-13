/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kpm.idea.proto

import org.jetbrains.kotlin.gradle.kpm.idea.*
import org.jetbrains.kotlin.gradle.kpm.idea.serialize.IdeaKpmSerializationContext

internal fun IdeaKpmSerializationContext.IdeaKpmPlatformProto(platform: IdeaKpmPlatform): IdeaKpmPlatformProto {
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

internal fun IdeaKpmSerializationContext.IdeaKpmPlatform(proto: IdeaKpmPlatformProto): IdeaKpmPlatform {
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

internal fun IdeaKpmSerializationContext.IdeaKpmJvmPlatformProto(platform: IdeaKpmJvmPlatform): IdeaKpmJvmPlatformProto {
    return ideaKpmJvmPlatformProto {
        if (platform.extras.isNotEmpty()) extras = IdeaKpmExtrasProto(platform.extras)
        jvmTarget = platform.jvmTarget
    }
}

internal fun IdeaKpmSerializationContext.IdeaKpmJvmPlatform(proto: IdeaKpmJvmPlatformProto): IdeaKpmJvmPlatform {
    return IdeaKpmJvmPlatformImpl(
        jvmTarget = proto.jvmTarget,
        extras = Extras(proto.extras)
    )
}

internal fun IdeaKpmSerializationContext.IdeaKpmJvmPlatform(data: ByteArray): IdeaKpmJvmPlatform {
    return IdeaKpmJvmPlatform(IdeaKpmJvmPlatformProto.parseFrom(data))
}

internal fun IdeaKpmJvmPlatform.toByteArray(context: IdeaKpmSerializationContext): ByteArray {
    return context.IdeaKpmJvmPlatformProto(this).toByteArray()
}

/* Native */

internal fun IdeaKpmSerializationContext.IdeaKpmNativePlatformProto(platform: IdeaKpmNativePlatform): IdeaKpmNativePlatformProto {
    return ideaKpmNativePlatformProto {
        if (platform.extras.isNotEmpty()) extras = IdeaKpmExtrasProto(platform.extras)
        konanTarget = platform.konanTarget
    }
}

internal fun IdeaKpmSerializationContext.IdeaKpmNativePlatform(proto: IdeaKpmNativePlatformProto): IdeaKpmNativePlatform {
    return IdeaKpmNativePlatformImpl(
        konanTarget = proto.konanTarget,
        extras = Extras(proto.extras)
    )
}

internal fun IdeaKpmSerializationContext.IdeaKpmNativePlatform(data: ByteArray): IdeaKpmNativePlatform {
    return IdeaKpmNativePlatform(IdeaKpmNativePlatformProto.parseFrom(data))
}

internal fun IdeaKpmNativePlatform.toByteArray(context: IdeaKpmSerializationContext): ByteArray {
    return context.IdeaKpmNativePlatformProto(this).toByteArray()
}

/* Js */

internal fun IdeaKpmSerializationContext.IdeaKpmJsPlatformProto(platform: IdeaKpmJsPlatform): IdeaKpmJsPlatformProto {
    return ideaKpmJsPlatformProto {
        if (platform.extras.isNotEmpty()) extras = IdeaKpmExtrasProto(platform.extras)
        isIr = platform.isIr
    }
}

internal fun IdeaKpmSerializationContext.IdeaKpmJsPlatform(proto: IdeaKpmJsPlatformProto): IdeaKpmJsPlatform {
    return IdeaKpmJsPlatformImpl(
        isIr = proto.isIr,
        extras = Extras(proto.extras)
    )
}

internal fun IdeaKpmSerializationContext.IdeaKpmJsPlatform(data: ByteArray): IdeaKpmJsPlatform {
    return IdeaKpmJsPlatform(IdeaKpmJsPlatformProto.parseFrom(data))
}

internal fun IdeaKpmJsPlatform.toByteArray(context: IdeaKpmSerializationContext): ByteArray {
    return context.IdeaKpmJsPlatformProto(this).toByteArray()
}

/* Wasm */

internal fun IdeaKpmSerializationContext.IdeaKpmWasmPlatformProto(platform: IdeaKpmWasmPlatform): IdeaKpmWasmPlatformProto {
    return ideaKpmWasmPlatformProto {
        if (platform.extras.isNotEmpty()) extras = IdeaKpmExtrasProto(platform.extras)
    }
}

internal fun IdeaKpmSerializationContext.IdeaKpmWasmPlatform(proto: IdeaKpmWasmPlatformProto): IdeaKpmWasmPlatform {
    return IdeaKpmWasmPlatformImpl(
        extras = Extras(proto.extras)
    )
}

internal fun IdeaKpmSerializationContext.IdeaKpmWasmPlatform(data: ByteArray): IdeaKpmWasmPlatform {
    return IdeaKpmWasmPlatform(IdeaKpmWasmPlatformProto.parseFrom(data))
}

internal fun IdeaKpmWasmPlatform.toByteArray(context: IdeaKpmSerializationContext): ByteArray {
    return context.IdeaKpmWasmPlatformProto(this).toByteArray()
}

/* Unknown */

internal fun IdeaKpmSerializationContext.IdeaKpmUnknownPlatformProto(platform: IdeaKpmUnknownPlatform): IdeaKpmUnknownPlatformProto {
    return ideaKpmUnknownPlatformProto {
        if (platform.extras.isNotEmpty()) extras = IdeaKpmExtrasProto(platform.extras)
    }
}

internal fun IdeaKpmSerializationContext.IdeaKpmUnknownPlatform(proto: IdeaKpmUnknownPlatformProto): IdeaKpmUnknownPlatform {
    return IdeaKpmUnknownPlatformImpl(
        extras = Extras(proto.extras)
    )
}

internal fun IdeaKpmSerializationContext.IdeaKpmUnknownPlatform(data: ByteArray): IdeaKpmUnknownPlatform {
    return IdeaKpmUnknownPlatform(IdeaKpmUnknownPlatformProto.parseFrom(data))
}

internal fun IdeaKpmUnknownPlatform.toByteArray(context: IdeaKpmSerializationContext): ByteArray {
    return context.IdeaKpmUnknownPlatformProto(this).toByteArray()
}
