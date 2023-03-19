package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.NormalizeLineEndings
import java.io.File

interface IncrementalSyncTask : Task {
    @get:InputFiles
    @get:NormalizeLineEndings
    @get:IgnoreEmptyDirectories
    @get:SkipWhenEmpty
    val from: ConfigurableFileCollection

    @get:OutputDirectory
    val destinationDirectory: Property<File>

    @get:Internal
    @Deprecated("Use destinationDirProperty with Provider API", ReplaceWith("destinationDirProperty.get()"))
    val destinationDir: File
        get() = destinationDirectory.get()
}