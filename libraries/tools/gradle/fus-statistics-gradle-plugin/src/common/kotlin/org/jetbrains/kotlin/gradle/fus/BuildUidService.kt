/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fus

import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.internal.RegisteredBuildServiceProvider
import org.gradle.util.GradleVersion
import java.util.UUID.randomUUID

abstract class BuildUidService : BuildService<BuildServiceParameters.None>, AutoCloseable {
    val buildId = randomUUID().toString()
    private val log = Logging.getLogger(this.javaClass)

    init {
        log.info("Build $buildId is started")
    }

    companion object {
        private val serviceName = "${BuildUidService::class.java.canonicalName}_${BuildUidService::class.java.classLoader.hashCode()}"

        fun registerIfAbsent(project: Project): Provider<BuildUidService> {
            return project.gradle.sharedServices.registerIfAbsent(serviceName, BuildUidService::class.java) {
            }.also {
                // There is a specific behavior in Gradle 8.0 where: the BuildUidService gets closed during the build execution process.
                // This results in having different buildIds for BuildCloseFusStatisticsBuildService and BuildFusService.
                if (GradleVersion.current().baseVersion >= GradleVersion.version("8.0")
                    && GradleVersion.current().baseVersion < GradleVersion.version("8.1")
                ) {
                    (it as RegisteredBuildServiceProvider<*, *>).keepAlive()
                }
            }
        }
    }

    override fun close() {
        log.info("Build $buildId is closed")
    }
}