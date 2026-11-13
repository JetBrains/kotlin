/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.targets.js.internal.RewriteSourceMapFilterReader
import java.io.File

/**
 * Rewrites relative paths in copied `.js.map`/`.mjs.map` files so they still resolve from the new location.
 *
 * [destinationDir] must match the spec's [CopySpec.into] directory.
 */
internal fun CopySpec.remapJavaScriptSourceMapSourcePaths(destinationDir: Provider<File>) {
    fun FileCopyDetails.isJavaScriptSourceMapFile() =
        name.endsWith(".js.map") || name.endsWith(".mjs.map")

    eachFile { details ->
        if (details.isJavaScriptSourceMapFile()) {
            details.filter(
                mapOf(
                    RewriteSourceMapFilterReader::srcSourceRoot.name to details.file.parentFile,
                    RewriteSourceMapFilterReader::targetSourceRoot.name to details.relativePath.getFile(destinationDir.get()).parentFile
                ),
                RewriteSourceMapFilterReader::class.java
            )
        }
    }
}
