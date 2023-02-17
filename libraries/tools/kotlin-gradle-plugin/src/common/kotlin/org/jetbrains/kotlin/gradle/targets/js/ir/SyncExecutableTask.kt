/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.OutputDirectory
import org.gradle.internal.hash.FileHasher
import org.jetbrains.kotlin.gradle.targets.js.internal.RewriteSourceMapFilterReader
import org.jetbrains.kotlin.gradle.targets.js.toHex
import java.io.File
import javax.inject.Inject

abstract class SyncExecutableTask : Copy() {

    @get:Inject
    abstract val fileHasher: FileHasher

    @get:Inject
    abstract val objects: ObjectFactory

    @get:OutputDirectory
    abstract val hashDir: Property<File>

    override fun copy() {
        val actualFiles = mutableSetOf<File>()

        val hashDirFile = hashDir.get()
        eachFile {
            actualFiles.add(it.relativeSourcePath.getFile(destinationDir))

            val hashFile = hashDirFile.resolve(it.relativeSourcePath.pathString + ".$HASH_EXTENSION")

            val currentHash = fileHasher.hash(it.file)
            val currentHashHex = currentHash.toByteArray().toHex()

            if (hashFile.exists()) {
                val previousHash = hashFile.readText()

                if (previousHash == currentHashHex) {
                    it.exclude()
                    return@eachFile
                }
            } else {
                hashFile.parentFile.mkdirs()
            }

            hashFile.writeText(currentHashHex)
        }

        // Rewrite relative paths in sourcemaps in the target directory
        eachFile {
            if (it.name.endsWith(".js.map")) {
                it.filter(
                    mapOf(
                        RewriteSourceMapFilterReader::srcSourceRoot.name to it.file.parentFile,
                        RewriteSourceMapFilterReader::targetSourceRoot.name to destinationDir
                    ),
                    RewriteSourceMapFilterReader::class.java
                )
            }
        }

        super.copy()

        objects.fileTree()
            .from(hashDirFile)
            .files
            .forEach {
                if (destinationDir.resolve(it.relativeTo(hashDirFile).path.removeSuffix(".$HASH_EXTENSION")) !in actualFiles) {
                    it.delete()
                }
            }

        objects.fileTree()
            .from(destinationDir)
            .files
            .forEach {
                if (it !in actualFiles) {
                    it.delete()
                }
            }
    }

    companion object {
        const val HASH_EXTENSION = "hash"
    }
}