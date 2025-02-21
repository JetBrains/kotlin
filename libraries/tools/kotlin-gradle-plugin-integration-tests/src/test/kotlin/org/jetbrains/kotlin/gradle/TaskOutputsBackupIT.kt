/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import javax.inject.Inject
import kotlin.io.path.*

@OtherGradlePluginTests
class TaskOutputsBackupIT : KGPBaseTest() {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)

    @GradleTest
    @DisplayName("Assert that TaskOutputsBackup can store and restore different types of output")
    fun basicSaveAndRestoreTest(version: GradleVersion) {

        project("empty", version) {
            val plainFile = projectPath.resolve("file1")
            plainFile.writeText("bar1")

            val dirWithFiles = projectPath.resolve("dirOut")
            dirWithFiles.toFile().mkdirs()

            val fileInDir2 = dirWithFiles.resolve("file2")
            fileInDir2.writeText("bar2")

            val fileInDir3 = dirWithFiles.resolve("file3")
            fileInDir3.writeText("bar3")

            val emptyDir = projectPath.resolve("dirOut2")
            emptyDir.toFile().mkdirs()

            val noFile = projectPath.resolve("noFile")
            assert(!noFile.toFile().exists())

            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                abstract class TaskOutputsBackupTestTask : DefaultTask() {
                    @get:Inject
                    abstract val fileSystemOperations: FileSystemOperations

                    @get:Inject
                    abstract val projectLayout: ProjectLayout

                    @get:Internal
                    abstract val snapshotDir: DirectoryProperty

                    @Internal
                    protected fun getTaskOutputsBackupInstance(): org.jetbrains.kotlin.gradle.tasks.TaskOutputsBackup {
                        val projectPath = projectLayout.projectDirectory.asFile
                        return org.jetbrains.kotlin.gradle.tasks.TaskOutputsBackup(
                            fileSystemOperations = fileSystemOperations,
                            buildDirectory = snapshotDir, // it's not actually used by TOB
                            snapshotsDir = snapshotDir,
                            outputsToRestore = listOf(
                                projectPath.resolve("file1"),
                                projectPath.resolve("dirOut"),
                                projectPath.resolve("dirOut2"),
                                projectPath.resolve("noFile"),
                            ),
                            logger = org.jetbrains.kotlin.gradle.logging.GradleKotlinLogger(
                                Logging.getLogger("task-outputs-backup"),
                            ),
                        )
                    }
                }

                abstract class MakeBackupTask : TaskOutputsBackupTestTask() {
                    @TaskAction
                    fun makeBackup() {
                        getTaskOutputsBackupInstance().createSnapshot()
                    }
                }
                project.tasks.register("makeBackup", MakeBackupTask::class.java) {
                    it.snapshotDir.convention(project.layout.projectDirectory.dir("snapshots"))
                }

                abstract class RestoreBackupTask : TaskOutputsBackupTestTask() {
                    @TaskAction
                    fun restoreBackup() {
                        getTaskOutputsBackupInstance().restoreOutputs()
                    }
                }
                project.tasks.register("restoreBackup", RestoreBackupTask::class.java) {
                    it.snapshotDir.convention(project.layout.projectDirectory.dir("snapshots"))
                }
            }

            build("makeBackup") {
                withFilteredOutput { filtered ->
                    assert(filtered.size == 4) // 4 outputs to save per config
                    assert(filtered[0].matches("Packing .*empty/dirOut as .*empty/snapshots/0.zip to make a backup".toRegex()))
                    assert(filtered[1].matches("Packing .*empty/dirOut2 as .*empty/snapshots/1.zip to make a backup".toRegex()))
                    assert(filtered[2].matches("Copying .*empty/file1 into .*empty/snapshots/2 to make a backup".toRegex()))
                    assert(filtered[3].matches("Creating is-empty marker file for .*empty/noFile as it does not exist".toRegex()))
                }
            }

            plainFile.deleteExisting()
            dirWithFiles.deleteRecursively()
            emptyDir.deleteRecursively()

            build("restoreBackup") {
                withFilteredOutput { filtered ->
                    assert(filtered.size == 4) // 4 outputs to recover per config
                    assert(filtered[0].matches("Unpacking .*empty/snapshots/0.zip into .*empty/dirOut to restore from backup".toRegex()))
                    assert(filtered[1].matches("Unpacking .*empty/snapshots/1.zip into .*empty/dirOut2 to restore from backup".toRegex()))
                    assert(filtered[2].matches("Copying file from .*empty/snapshots/2 into .*empty to restore file1 from backup".toRegex()))
                    assert(filtered[3].matches("Found marker .*empty/snapshots/3.not-exists for .*/empty/noFile, doing nothing".toRegex()))
                }
            }

            assert(plainFile.readLines().firstOrNull() == "bar1") { "failed to restore plain file" }

            assert(emptyDir.isDirectory() && emptyDir.listDirectoryEntries().isEmpty()) { "failed to restore empty directory" }

            assert(!noFile.exists()) { "erroneously created file by empty marker" }

            assert(dirWithFiles.isDirectory()) { "failed to restore directory with files" }
            assert(fileInDir2.readLines().firstOrNull() == "bar2") { "failed to restore directory with files: bad contents" }
            assert(fileInDir3.readLines().firstOrNull() == "bar3") { "failed to restore directory with files: bad contents" }
        }
    }

    private fun BuildResult.withFilteredOutput(assertions: (filteredOutput: List<String>) -> Unit) {
        val filtered = output.split("\n").filter {
            it.contains("[task-outputs-backup] ")
        }.map {
            it.substringAfter("[task-outputs-backup] ").trim()
        }
        try {
            assertions(filtered)
        } catch (e: AssertionError) {
            println(filtered.joinToString("\n"))
            throw e
        }
    }
}
