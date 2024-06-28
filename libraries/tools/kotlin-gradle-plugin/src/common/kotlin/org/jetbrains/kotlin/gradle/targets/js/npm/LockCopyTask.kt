/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault
abstract class LockCopyTask : DefaultTask() {

    @get:NormalizeLineEndings
    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputFile: RegularFileProperty

    @get:NormalizeLineEndings
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val additionalInputFiles: ConfigurableFileCollection = project.objects.fileCollection()

    @get:Internal
    abstract val outputDirectory: DirectoryProperty

    @get:Internal
    abstract val fileName: Property<String>

    @get:OutputFile
    val outputFile: Provider<File>
        get() = outputDirectory.map { dir ->
            dir.asFile.resolve(fileName.get())
        }

    @get:Inject
    abstract val fs: FileSystemOperations

    @TaskAction
    open fun copy() {
        fs.copy { copy ->
            inputFile.getOrNull()?.let { inputFile ->
                copy.from(inputFile) {
                    it.rename { fileName.get() }
                }
            }

            copy.from(additionalInputFiles)
            copy.into(outputDirectory)
        }
    }

    companion object {
        const val STORE_PACKAGE_LOCK_NAME = "kotlinStorePackageLock"
        const val RESTORE_PACKAGE_LOCK_NAME = "kotlinRestorePackageLock"
        const val UPGRADE_PACKAGE_LOCK = "kotlinUpgradePackageLock"
        const val PACKAGE_LOCK_MISMATCH_MESSAGE = "Lock file was changed. Run the `$UPGRADE_PACKAGE_LOCK` task to actualize lock file"
        const val KOTLIN_JS_STORE = "kotlin-js-store"
        const val PACKAGE_LOCK = "package-lock.json"
        const val YARN_LOCK = "yarn.lock"
    }
}

enum class LockFileMismatchReport {
    NONE,
    WARNING,
    FAIL,
}