package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.NormalizeLineEndings
import java.io.File

/**
 * A task to incrementally synchronize a set of files between directories.
 *
 * Incremental synchronization support greatly reduces task execution time on subsequent builds when the set of files to be synchronized is large,
 * but only a small amount have changed.
 */
interface IncrementalSyncTask : Task {

    /**
     * The collection of paths with files to copy.
     *
     * Should be configured using available methods in the [ConfigurableFileCollection]
     * such as [ConfigurableFileCollection.from] or [ConfigurableFileCollection.setFrom].
     *
     * @see [ConfigurableFileCollection]
     */
    @get:InputFiles
    @get:NormalizeLineEndings
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:SkipWhenEmpty
    val from: ConfigurableFileCollection

    /**
     * The directory where the set of files are copied to.
     */
    @get:OutputDirectory
    val destinationDirectory: Property<File>

    /**
     * @suppress
     */
    @get:Internal
    @Deprecated("Use destinationDirectory with Provider API", ReplaceWith("destinationDirectory.get()"))
    val destinationDir: File
        get() = destinationDirectory.get()
}