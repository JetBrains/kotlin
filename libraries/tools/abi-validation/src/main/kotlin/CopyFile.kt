/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

// Built-in Gradle's Copy/Sync tasks accepts only a destination directory (not a single file)
// and registers it as an output dependency. If there's another task reading from that particular
// directory or writing into it, their input/output dependencies would clash and as long as
// there will be no explicit ordering or dependencies between these tasks, Gradle would be unhappy.
internal open class CopyFile : DefaultTask() {
    @InputFiles
    lateinit var from: File

    @OutputFile
    lateinit var to: File

    @TaskAction
    fun copy() {
        Files.copy(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}
