/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.internal

import java.io.File

/**
 * Wrapper around [File] to be able to mark [store] storage as used by calling [use]`()`.
 */
class DownloadedFile internal constructor(
    private val store: CleanableStore,
    private val file: File,
) {
    fun use(): File {
        store.markUsed()
        return file
    }

    fun resolve(fileName: String): DownloadedFile =
        DownloadedFile(store, file.resolve(fileName))
}