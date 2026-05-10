/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.internal

import java.io.Serializable
import java.time.Instant

/**
 * Store of tracked directories that can be cleaned with [org.jetbrains.kotlin.gradle.tasks.CleanDataTask].
 * All directories that was not marked as used at least [org.jetbrains.kotlin.gradle.tasks.CleanDataTask.timeToLiveInDays] days will be removed.
 *
 * To register store call `CleanableStore["/path/to/dir"]`.
 * Now you will be able to access files via `CleanableStore["/path/to/dir"]["file/name"].use()`
 * and it would update usage of th store.
 */
@Deprecated("Removed in Kotlin 2.4", level = DeprecationLevel.ERROR)
@Suppress("DEPRECATION_ERROR")
interface CleanableStore : Serializable {
    fun cleanDir(expirationDate: Instant)

    operator fun get(fileName: String): DownloadedFile

    fun markUsed()
}