/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import java.nio.file.Files
import java.nio.file.StandardCopyOption

// Built-in Gradle's Copy/Sync tasks accepts only a destination directory (not a single file)
// and registers it as an output dependency. If there's another task reading from that particular
// directory or writing into it, their input/output dependencies would clash and as long as
// there will be no explicit ordering or dependencies between these tasks, Gradle would be unhappy.
@DisableCachingByDefault(because = "No computations, only copying files")
internal abstract class SyncFile : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val from: RegularFileProperty

    @get:OutputFile
    abstract val to: RegularFileProperty

    @TaskAction
    fun copy() {
        val fromFile = from.asFile.get()
        val toFile = to.asFile.get()
        if (fromFile.exists()) {
            fromFile.copyTo(toFile, overwrite = true)
        } else {
            Files.deleteIfExists(toFile.toPath())
        }
    }
}
