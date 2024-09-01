/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.NativeCacheKind
import org.jetbrains.kotlin.gradle.dsl.NativeCacheOrchestration
import org.jetbrains.kotlin.gradle.internal.properties.NativeProperties
import org.jetbrains.kotlin.gradle.internal.properties.nativeProperties
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.targets.native.KonanPropertiesBuildService
import org.jetbrains.kotlin.konan.target.KonanTarget

internal fun Project.getKonanCacheKind(target: KonanTarget): Provider<NativeCacheKind> =
    nativeProperties.getKonanCacheKind(target, KonanPropertiesBuildService.registerIfAbsent(this))

@Suppress("UNCHECKED_CAST")
internal fun NativeProperties.getKonanCacheKind(
    target: KonanTarget,
    konanPropertiesBuildService: Provider<KonanPropertiesBuildService>
): Provider<NativeCacheKind> = nativeCacheKind
    .orElse(nativeCacheKindForTarget(target))
    .orElse(konanPropertiesBuildService.map { it.defaultCacheKindForTarget(target) }) as Provider<NativeCacheKind>

internal fun Project.getKonanCacheOrchestration(): NativeCacheOrchestration {
    return PropertiesProvider(this).nativeCacheOrchestration ?: NativeCacheOrchestration.Compiler
}

internal fun Project.isKonanIncrementalCompilationEnabled(): Boolean {
    return PropertiesProvider(this).incrementalNative ?: false
}

internal fun Project.getKonanParallelThreads(): Int {
    return PropertiesProvider(this).nativeParallelThreads ?: 4
}
