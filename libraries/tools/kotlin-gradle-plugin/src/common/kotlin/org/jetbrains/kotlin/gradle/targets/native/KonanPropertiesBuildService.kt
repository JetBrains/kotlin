/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.gradle.dsl.NativeCacheKind
import org.jetbrains.kotlin.gradle.internal.properties.nativeProperties
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.SingleActionPerProject
import org.jetbrains.kotlin.gradle.utils.registerClassLoaderScopedBuildService
import org.jetbrains.kotlin.konan.properties.resolvablePropertyList
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.util.*

internal interface UsesKonanPropertiesBuildService : Task {
    @get:Internal
    val konanPropertiesService: Property<KonanPropertiesBuildService>
}

internal val Project.konanPropertiesBuildService
    get() = KonanPropertiesBuildService.registerIfAbsent(this)

abstract class KonanPropertiesBuildService : BuildService<KonanPropertiesBuildService.Parameters> {

    internal interface Parameters : BuildServiceParameters {
        val konanHome: DirectoryProperty
    }

    private val logger = Logging.getLogger(this::class.java)

    private val properties: Properties by lazy {
        Distribution(parameters.konanHome.get().asFile.absolutePath).properties
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

    internal val environmentBlacklist: Set<String> by lazy {
        val envBlacklistFile = parameters.konanHome.get().asFile.resolve("tools/env_blacklist")
        if (envBlacklistFile.exists()) {
            envBlacklistFile.readLines().toSet()
        } else {
            logger.warn("Can't find env_blacklist file at $envBlacklistFile.")
            emptySet()
        }
    }

    companion object {
        fun registerIfAbsent(project: Project): Provider<KonanPropertiesBuildService> = project.gradle
            .registerClassLoaderScopedBuildService(KonanPropertiesBuildService::class) {
                it.parameters.konanHome.fileProvider(project.nativeProperties.actualNativeHomeDirectory).disallowChanges()
            }.also { serviceProvider ->
                SingleActionPerProject.run(project, UsesKonanPropertiesBuildService::class.java.name) {
                    project.tasks.withType<UsesKonanPropertiesBuildService>().configureEach { task ->
                        task.usesService(serviceProvider)
                    }
                }
            }
    }
}