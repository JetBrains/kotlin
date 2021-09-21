/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.internal

import org.jetbrains.kotlin.gradle.tasks.CleanDataTask
import java.io.Serializable
import java.time.Instant

/**
 * Store of tracked directories that can be cleaned with [CleanDataTask].
 * All directories that was not marked as used at least [CleanDataTask.timeToLiveInDays] days will be removed.
 *
 * To register store call `CleanableStore["/path/to/dir"]`.
 * Now you will be able to access files via `CleanableStore["/path/to/dir"]["file/name"].use()`
 * and it would update usage of th store.
 */
interface CleanableStore : Serializable {
    fun cleanDir(expirationDate: Instant)

    operator fun get(fileName: String): DownloadedFile

    fun markUsed()

    companion object {
        private val mutableStores = mutableMapOf<String, CleanableStore>()

        val stores: Map<String, CleanableStore>
            get() = mutableStores.toMap()

        operator fun get(path: String): CleanableStore =
            mutableStores.getOrPut(path) { CleanableStoreImpl(path) }
    }
}