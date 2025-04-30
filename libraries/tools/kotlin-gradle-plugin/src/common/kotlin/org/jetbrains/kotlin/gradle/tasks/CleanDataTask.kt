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
import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore
import java.time.Duration
import java.time.Instant

/**
 * Task to clean all old unused loaded files from registered stores in [CleanableStore].
 */
@DisableCachingByDefault
open class CleanDataTask : DefaultTask() {

    /**
     * Path to folder.
     * Use path instead of file to avoid file scanning for change check
     */
    @Input
    lateinit var cleanableStoreProvider: Provider<CleanableStore>

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

        const val DEPRECATION_MESSAGE = "The task type is deprecated. Scheduled for removal in Kotlin 2.4."
    }

}