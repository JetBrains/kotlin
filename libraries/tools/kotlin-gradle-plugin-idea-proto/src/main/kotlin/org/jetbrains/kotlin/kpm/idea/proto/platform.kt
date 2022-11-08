/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kpm.idea.proto

import org.jetbrains.kotlin.gradle.idea.kpm.*
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaSerializationContext

internal fun IdeaSerializationContext.IdeaKpmPlatformProto(platform: IdeaKpmPlatform): IdeaKpmPlatformProto {
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

internal fun IdeaSerializationContext.IdeaKpmPlatform(proto: IdeaKpmPlatformProto): IdeaKpmPlatform {
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

internal fun IdeaSerializationContext.IdeaKpmJvmPlatformProto(platform: IdeaKpmJvmPlatform): IdeaKpmJvmPlatformProto {
    return ideaKpmJvmPlatformProto {
        if (platform.extras.isNotEmpty()) extras = IdeaKpmExtrasProto(platform.extras)
        jvmTarget = platform.jvmTarget
    }
}

internal fun IdeaSerializationContext.IdeaKpmJvmPlatform(proto: IdeaKpmJvmPlatformProto): IdeaKpmJvmPlatform {
    return IdeaKpmJvmPlatformImpl(
        jvmTarget = proto.jvmTarget,
        extras = Extras(proto.extras)
    )
}

internal fun IdeaSerializationContext.IdeaKpmJvmPlatform(data: ByteArray): IdeaKpmJvmPlatform {
    return IdeaKpmJvmPlatform(IdeaKpmJvmPlatformProto.parseFrom(data))
}

internal fun IdeaKpmJvmPlatform.toByteArray(context: IdeaSerializationContext): ByteArray {
    return context.IdeaKpmJvmPlatformProto(this).toByteArray()
}

/* Native */

internal fun IdeaSerializationContext.IdeaKpmNativePlatformProto(platform: IdeaKpmNativePlatform): IdeaKpmNativePlatformProto {
    return ideaKpmNativePlatformProto {
        if (platform.extras.isNotEmpty()) extras = IdeaKpmExtrasProto(platform.extras)
        konanTarget = platform.konanTarget
    }
}

internal fun IdeaSerializationContext.IdeaKpmNativePlatform(proto: IdeaKpmNativePlatformProto): IdeaKpmNativePlatform {
    return IdeaKpmNativePlatformImpl(
        konanTarget = proto.konanTarget,
        extras = Extras(proto.extras)
    )
}

internal fun IdeaSerializationContext.IdeaKpmNativePlatform(data: ByteArray): IdeaKpmNativePlatform {
    return IdeaKpmNativePlatform(IdeaKpmNativePlatformProto.parseFrom(data))
}

internal fun IdeaKpmNativePlatform.toByteArray(context: IdeaSerializationContext): ByteArray {
    return context.IdeaKpmNativePlatformProto(this).toByteArray()
}

/* Js */

internal fun IdeaSerializationContext.IdeaKpmJsPlatformProto(platform: IdeaKpmJsPlatform): IdeaKpmJsPlatformProto {
    return ideaKpmJsPlatformProto {
        if (platform.extras.isNotEmpty()) extras = IdeaKpmExtrasProto(platform.extras)
        isIr = platform.isIr
    }
}

internal fun IdeaSerializationContext.IdeaKpmJsPlatform(proto: IdeaKpmJsPlatformProto): IdeaKpmJsPlatform {
    return IdeaKpmJsPlatformImpl(
        isIr = proto.isIr,
        extras = Extras(proto.extras)
    )
}

internal fun IdeaSerializationContext.IdeaKpmJsPlatform(data: ByteArray): IdeaKpmJsPlatform {
    return IdeaKpmJsPlatform(IdeaKpmJsPlatformProto.parseFrom(data))
}

internal fun IdeaKpmJsPlatform.toByteArray(context: IdeaSerializationContext): ByteArray {
    return context.IdeaKpmJsPlatformProto(this).toByteArray()
}

/* Wasm */

internal fun IdeaSerializationContext.IdeaKpmWasmPlatformProto(platform: IdeaKpmWasmPlatform): IdeaKpmWasmPlatformProto {
    return ideaKpmWasmPlatformProto {
        if (platform.extras.isNotEmpty()) extras = IdeaKpmExtrasProto(platform.extras)
    }
}

internal fun IdeaSerializationContext.IdeaKpmWasmPlatform(proto: IdeaKpmWasmPlatformProto): IdeaKpmWasmPlatform {
    return IdeaKpmWasmPlatformImpl(
        extras = Extras(proto.extras)
    )
}

internal fun IdeaSerializationContext.IdeaKpmWasmPlatform(data: ByteArray): IdeaKpmWasmPlatform {
    return IdeaKpmWasmPlatform(IdeaKpmWasmPlatformProto.parseFrom(data))
}

internal fun IdeaKpmWasmPlatform.toByteArray(context: IdeaSerializationContext): ByteArray {
    return context.IdeaKpmWasmPlatformProto(this).toByteArray()
}

/* Unknown */

internal fun IdeaSerializationContext.IdeaKpmUnknownPlatformProto(platform: IdeaKpmUnknownPlatform): IdeaKpmUnknownPlatformProto {
    return ideaKpmUnknownPlatformProto {
        if (platform.extras.isNotEmpty()) extras = IdeaKpmExtrasProto(platform.extras)
    }
}

internal fun IdeaSerializationContext.IdeaKpmUnknownPlatform(proto: IdeaKpmUnknownPlatformProto): IdeaKpmUnknownPlatform {
    return IdeaKpmUnknownPlatformImpl(
        extras = Extras(proto.extras)
    )
}

internal fun IdeaSerializationContext.IdeaKpmUnknownPlatform(data: ByteArray): IdeaKpmUnknownPlatform {
    return IdeaKpmUnknownPlatform(IdeaKpmUnknownPlatformProto.parseFrom(data))
}

internal fun IdeaKpmUnknownPlatform.toByteArray(context: IdeaSerializationContext): ByteArray {
    return context.IdeaKpmUnknownPlatformProto(this).toByteArray()
}
