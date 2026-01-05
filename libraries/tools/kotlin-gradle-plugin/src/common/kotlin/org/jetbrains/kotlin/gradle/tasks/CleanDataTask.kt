/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.tasks.internal.cleanDir
import java.time.Duration
import java.time.Instant

/**
 * Task to clean all old unused loaded files from [storeProvider].
 */
@DisableCachingByDefault
@Deprecated("Scheduled for removal in Kotlin 2.4", level = DeprecationLevel.WARNING)
open class CleanDataTask : DefaultTask() {
    /**
     * Path to folder.
     * Use path instead of file to avoid file scanning for change check
     */
    @Suppress("DEPRECATION")
    @Input
    lateinit var cleanableStoreProvider: Provider<org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore>

    /**
     * Time to live in days
     */
    @Input
    var timeToLiveInDays: Long = 30

    @Suppress("unused")
    @TaskAction
    fun exec() {
        val expirationDate = Instant.now().minus(Duration.ofDays(timeToLiveInDays))
        cleanableStoreProvider.get().cleanDir(expirationDate)
    }

    companion object {
        const val NAME_SUFFIX: String = "KotlinClean"

        @InternalKotlinGradlePluginApi
        fun deprecationMessage(taskPath: String) = "The task '$taskPath' is deprecated. Scheduled for removal in Kotlin 2.4."
    }

}