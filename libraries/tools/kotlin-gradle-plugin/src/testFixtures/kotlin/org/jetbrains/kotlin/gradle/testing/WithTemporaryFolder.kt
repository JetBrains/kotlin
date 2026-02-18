/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testing

import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile

/**
 * Helper interface for tests working with a temporary folder.
 * Brings junit4 like utility methods like [newTempDirectory] and [newTempFile]
 */
interface WithTemporaryFolder {
    val temporaryFolder: Path
}

fun WithTemporaryFolder.newTempDirectory(prefix: String? = null): Path = createTempDirectory(temporaryFolder, prefix = prefix)

fun WithTemporaryFolder.newTempFile(name: String? = null): Path = if (name != null) temporaryFolder.resolve(name).also { it.createFile() }
else createTempFile(temporaryFolder)
