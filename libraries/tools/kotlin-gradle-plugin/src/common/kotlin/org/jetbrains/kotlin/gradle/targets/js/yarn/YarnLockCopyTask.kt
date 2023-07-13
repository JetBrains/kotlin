/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.utils.contentEquals
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault
abstract class YarnLockCopyTask : DefaultTask() {

    @get:NormalizeLineEndings
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputFile: RegularFileProperty

    @get:Internal
    abstract val outputDirectory: RegularFileProperty

    @get:Internal
    abstract val fileName: Property<String>

    @get:OutputFile
    val outputFile: Provider<File>
        get() = outputDirectory.map { regularFile ->
            regularFile.asFile.resolve(fileName.get())
        }

    @get:Inject
    abstract val fs: FileSystemOperations

    @TaskAction
    open fun copy() {
        fs.copy { copy ->
            copy.from(inputFile)
            copy.rename { fileName.get() }
            copy.into(outputDirectory)
        }
    }

    companion object {
        val STORE_YARN_LOCK_NAME = "kotlinStoreYarnLock"
        val RESTORE_YARN_LOCK_NAME = "kotlinRestoreYarnLock"
        val UPGRADE_YARN_LOCK = "kotlinUpgradeYarnLock"
        val YARN_LOCK_MISMATCH_MESSAGE = "yarn.lock was changed. Run the `${YarnLockCopyTask.UPGRADE_YARN_LOCK}` task to actualize yarn.lock file"
    }
}

@DisableCachingByDefault
abstract class YarnLockStoreTask : YarnLockCopyTask() {
    @Input
    lateinit var yarnLockMismatchReport: Provider<YarnLockMismatchReport>

    @Input
    lateinit var reportNewYarnLock: Provider<Boolean>

    @Input
    lateinit var yarnLockAutoReplace: Provider<Boolean>

    override fun copy() {
        val outputFile = outputDirectory.get().asFile.resolve(fileName.get())

        val shouldReportMismatch = if (!outputFile.exists()) {
            reportNewYarnLock.get()
        } else {
            yarnLockMismatchReport.get() != YarnLockMismatchReport.NONE && !contentEquals(inputFile.get().asFile, outputFile)
        }

        if (!outputFile.exists() || yarnLockAutoReplace.get()) {
            super.copy()
        }

        if (shouldReportMismatch) {
            when (yarnLockMismatchReport.get()) {
                YarnLockMismatchReport.NONE -> {}
                YarnLockMismatchReport.WARNING -> {
                    logger.warn(YARN_LOCK_MISMATCH_MESSAGE)
                }
                YarnLockMismatchReport.FAIL -> throw GradleException(YARN_LOCK_MISMATCH_MESSAGE)
                else -> error("Unknown yarn.lock mismatch report kind")
            }
        }
    }
}

enum class YarnLockMismatchReport {
    NONE,
    WARNING,
    FAIL,
}