/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.kgpnpmtooling.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.NONE
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import javax.inject.Inject

/**
 * Copies the lockfiles from the [npmToolingProjectDir] to the [outputDir].
 */
@DisableCachingByDefault(because = "Simple file operations, caching has no benefits.")
abstract class PrepareNpmToolingLockFilesTask
@Inject
internal constructor(
    private val fs: FileSystemOperations,
) : DefaultTask() {

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(NONE)
    @get:NormalizeLineEndings
    val npmLockFile: Provider<RegularFile>
        get() = npmToolingProjectDir.file("package-lock.json")

    @get:InputFile
    @get:PathSensitive(NONE)
    @get:NormalizeLineEndings
    val yarnLockFile: Provider<RegularFile>
        get() = npmToolingProjectDir.file("yarn.lock")

    @get:Input
    abstract val relativePathToBaseLockFilesDir: Property<String>

    @get:Internal
    abstract val npmToolingProjectDir: DirectoryProperty

    @TaskAction
    protected fun action() {
        val outputDir = outputDir.get().asFile
        val lockfileDir = outputDir.resolve(relativePathToBaseLockFilesDir.get())

        fs.sync { s ->
            s.from(npmLockFile) {
                it.into("npm")
            }
            s.from(yarnLockFile) {
                it.into("yarn")
            }
            s.into(lockfileDir)
        }
    }
}
