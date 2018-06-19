package org.jetbrains.kotlin.gradle.plugin.experimental.internal

import org.gradle.api.internal.component.UsageContext
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.language.cpp.internal.NativeVariantIdentity
import org.jetbrains.kotlin.konan.target.KonanTarget

open class KotlinNativeVariantIdentity(
        name: String,
        baseName: Provider<String>,
        group: Provider<String>,
        version: Provider<String>,
        val konanTarget: KonanTarget,
        debuggable: Boolean,
        optimized: Boolean,
        linkUsage: UsageContext?,
        runtimeUsage: UsageContext?,
        objects: ObjectFactory
) : NativeVariantIdentity(
        name,
        baseName,
        group,
        version,
        debuggable,
        optimized,
        konanTarget.getGradleOSFamily(objects),
        linkUsage,
        runtimeUsage
) {
    val targetPlatform = DefaultKotlinNativePlatform(konanTarget)
}