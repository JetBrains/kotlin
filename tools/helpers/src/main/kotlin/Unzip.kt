/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.konan

import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

fun Path.unzipTo(directory: Path) {
    val zipUri = URI.create("jar:" + this.toUri())
    FileSystems.newFileSystem(zipUri, emptyMap<String, Any?>(), null).use { zipfs ->
        val zipPath = zipfs.getPath("/")
        zipPath.recursiveCopyTo(directory)
    }
}

fun Path.recursiveCopyTo(destPath: Path) {
    val sourcePath = this
    Files.walk(sourcePath).forEach { oldPath ->

        val relative = sourcePath.relativize(oldPath)

        // We are copying files between file systems,
        // so pass the relative path through the Sting.
        val newPath = destPath.resolve(relative.toString())

        if (Files.isDirectory(oldPath)) {
            Files.createDirectories(newPath)
            // TODO: consider copying attributes.
        } else {
            Files.copy(oldPath, newPath,
                    StandardCopyOption.REPLACE_EXISTING)
        }
    }
}