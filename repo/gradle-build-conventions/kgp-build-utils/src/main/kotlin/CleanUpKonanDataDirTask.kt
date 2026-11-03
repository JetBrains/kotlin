/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalPathApi::class)

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.register
import java.time.Duration
import java.time.Instant
import kotlin.io.path.*

/**
 * Deletes K/N distributions in [konanDataDir] that are older than [maxDistributionAge],
 * and the native dependencies with them, so unused versions don't pile up.
 */
@UntrackedTask(because = "Deletes outdated files, depending on the current time")
abstract class CleanUpKonanDataDirTask internal constructor() : DefaultTask() {

    /**
     * The konan data dir, containing `kotlin-native-prebuilt-*` distributions and their `dependencies`.
     */
    @get:Internal
    abstract val konanDataDir: DirectoryProperty

    /**
     * Distributions whose directory was last modified longer ago than this are deleted.
     */
    @get:Input
    abstract val maxDistributionAge: Property<Duration>

    @TaskAction
    protected fun cleanUp() {
        val konanDataDir = konanDataDir.get().asFile.toPath()
        if (!konanDataDir.exists()) return

        val oldestAllowed = Instant.now() - maxDistributionAge.get()
        val outdatedDistributions = konanDataDir.listDirectoryEntries("kotlin-native-prebuilt-*")
            .filter { distribution ->
                val provisionedAt = distribution.getLastModifiedTime()
                provisionedAt.toInstant() < oldestAllowed
            }
        if (outdatedDistributions.isEmpty()) return

        outdatedDistributions.forEach { distribution ->
            logger.lifecycle("Deleting outdated K/N distribution $distribution")
            distribution.deleteRecursively()
        }

        // Native dependencies are shared between distributions, and working out which ones are still
        // required from konan.properties isn't reliable. They're only a download cache, so drop them all.
        val dependencies = konanDataDir.resolve("dependencies")
        logger.lifecycle("Deleting K/N dependencies $dependencies")
        dependencies.deleteRecursively()
    }

    companion object {
        fun Project.registerCleanUpKonanDataDirTask(
            isTeamCityBuild: Provider<Boolean>,
            konanDataDirForIntegrationTests: Directory,
            maxDistributionAge: Duration = Duration.ofDays(14),
        ): TaskProvider<CleanUpKonanDataDirTask> {
            return tasks.register<CleanUpKonanDataDirTask>("cleanUpKonanForGradleTests") {
                description = "Deletes K/N distributions in the IT konan data dir based on age, " +
                        "and the native dependencies with them, so unused versions don't pile up and waste disk space. " +
                        "Only runs locally."
                onlyIf("not on TeamCity") { !isTeamCityBuild.get() }
                this.konanDataDir.set(konanDataDirForIntegrationTests)
                this.maxDistributionAge.set(maxDistributionAge)
            }
        }
    }
}
