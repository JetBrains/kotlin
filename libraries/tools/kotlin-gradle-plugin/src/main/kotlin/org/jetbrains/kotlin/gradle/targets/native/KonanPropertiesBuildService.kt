/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native

import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.gradle.dsl.NativeCacheKind
import org.jetbrains.kotlin.konan.properties.resolvablePropertyList
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.util.*

abstract class KonanPropertiesBuildService : BuildService<KonanPropertiesBuildService.Parameters> {

    internal interface Parameters : BuildServiceParameters {
        val konanHome: Property<String>
    }

    private val properties: Properties by lazy {
        Distribution(parameters.konanHome.get()).properties
    }

    private val cacheableTargets: List<KonanTarget> by lazy {
        properties
            .resolvablePropertyList("cacheableTargets", HostManager.hostName)
            .map { KonanTarget.predefinedTargets.getValue(it) }
    }

    private val targetsWithOptInStaticCaches: List<KonanTarget> by lazy {
        properties
            .resolvablePropertyList("optInCacheableTargets", HostManager.hostName)
            .map { KonanTarget.predefinedTargets.getValue(it) }
    }

    internal fun defaultCacheKindForTarget(target: KonanTarget): NativeCacheKind =
        if (target in cacheableTargets && target !in targetsWithOptInStaticCaches) {
            NativeCacheKind.STATIC
        } else {
            NativeCacheKind.NONE
        }

    internal fun cacheWorksFor(target: KonanTarget): Boolean =
        target in cacheableTargets

    internal fun additionalCacheFlags(target: KonanTarget): List<String> =
        properties.resolvablePropertyList("additionalCacheFlags", target.visibleName)

    internal val compilerVersion: String? by lazy {
        properties["compilerVersion"]?.toString()
    }

    companion object {
        fun registerIfAbsent(gradle: Gradle): Provider<KonanPropertiesBuildService> =
            gradle.sharedServices.registerIfAbsent(serviceName, KonanPropertiesBuildService::class.java) { service ->
                service.parameters.konanHome.set(gradle.rootProject.konanHome)
            }

        private val serviceName: String
            get() {
                val clazz = KonanPropertiesBuildService::class.java
                return "${clazz}_${clazz.classLoader.hashCode()}"
            }
    }
}