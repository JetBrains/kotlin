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
import org.jetbrains.kotlin.gradle.utils.property
import java.time.Instant

/**
 * Task to clean all old unused loaded files from [storeProvider].
 */
@DisableCachingByDefault
@Deprecated("Scheduled for removal in Kotlin 2.4", level = DeprecationLevel.ERROR)
open class CleanDataTask : DefaultTask() {

    /**
     * Path to folder.
     * Use path instead of file to avoid file scanning for change check
     */
    @Suppress("DEPRECATION_ERROR")
    @Deprecated("Scheduled for removal in Kotlin 2.4", level = DeprecationLevel.ERROR)
    @Input
    var cleanableStoreProvider: Provider<org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore> =
        project.objects.property(CleanableStoreObject)

    /**
     * Time to live in days
     */
    @Suppress("unused")
    @Deprecated("Scheduled for removal in Kotlin 2.4", level = DeprecationLevel.ERROR)
    @Input
    var timeToLiveInDays: Long = 30

    @Suppress("unused")
    @TaskAction
    fun exec() {
        throw UnsupportedOperationException(deprecationMessage(path))
    }

    companion object {
        const val NAME_SUFFIX: String = "KotlinClean"

        @InternalKotlinGradlePluginApi
        fun deprecationMessage(taskPath: String) = "The task '$taskPath' is deprecated. Scheduled for removal in Kotlin 2.4."

        @Suppress("DEPRECATION_ERROR")
        private object CleanableStoreObject : org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore {
            private fun readResolve(): Any = CleanableStoreObject
            override fun cleanDir(expirationDate: Instant) {
                throw UnsupportedOperationException("CleanableStore is scheduled for removal in Kotlin 2.4")
            }

            override fun get(fileName: String): org.jetbrains.kotlin.gradle.tasks.internal.DownloadedFile {
                throw UnsupportedOperationException("CleanableStore is scheduled for removal in Kotlin 2.4")
            }

            override fun markUsed() {
                throw UnsupportedOperationException("CleanableStore is scheduled for removal in Kotlin 2.4")
            }
        }
    }
}

