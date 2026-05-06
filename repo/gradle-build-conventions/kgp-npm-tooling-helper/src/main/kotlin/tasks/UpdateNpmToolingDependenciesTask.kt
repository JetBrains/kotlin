/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.kgpnpmtooling.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.NONE
import org.gradle.api.tasks.options.Option
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.build.kgpnpmtooling.internal.execCapture
import java.io.File
import javax.inject.Inject

/**
 * Updates the `package-lock.json` and `yarn.lock` files.
 *
 * If [updateVersions] is `true`, updates all dependencies in
 * the `package.json` file to the latest versions.
 *
 * ---
 *
 * This task is a convenience task and should be run manually.
 * Do not run this task as part of the regular KGP build.
 * It doesn't run all the time because we only need to update dependencies as needed.
 * Eagerly updating will cause unnecessary cache misses, or
 * risk introducing new dependencies that haven't been thoroughly tested yet.
 */
@UntrackedTask(because = "Attempting to upgrade versions should always run.")
abstract class UpdateNpmToolingDependenciesTask
@Inject
internal constructor(
    private val exec: ExecOperations,
) : DefaultTask() {

    @get:OutputFile
    val packageJson: Provider<RegularFile>
        get() = npmToolingProjectDir.file("package.json")

    @get:OutputFile
    val npmLockFile: Provider<RegularFile>
        get() = npmToolingProjectDir.file("package-lock.json")

    @get:OutputFile
    val yarnLockFile: Provider<RegularFile>
        get() = npmToolingProjectDir.file("yarn.lock")

    @get:Option("update-versions", description = "Whether to update the versions in the package.json file.")
    @get:Input
    @get:Optional
    abstract val updateVersions: Property<Boolean>

    /** Location of the `node` executable. */
    @get:InputFile
    @get:PathSensitive(NONE)
    abstract val nodeExecutable: RegularFileProperty

    /** Location of the `npm` executable. */
    @get:InputFile
    @get:PathSensitive(NONE)
    abstract val npmCli: RegularFileProperty

    /** Location of the `yarn` executable. */
    @get:InputFile
    @get:PathSensitive(NONE)
    abstract val yarnCli: RegularFileProperty

    @get:Internal
    abstract val npmToolingProjectDir: DirectoryProperty

    /**
     * Common base directory for all work directories.
     * Each operation runs in an independent subdirectory to avoid conflicts.
     */
    private val baseWorkDir: File get() = temporaryDir

    @TaskAction
    protected fun action() {
        if (updateVersions.orNull == true) {
            updateDependenciesVersions()
        }
        executeNpmInstall()
        executeYarnInstall()
    }

    /**
     * Upgrade all versions in `package.json` using `npm update --save`.
     */
    private fun updateDependenciesVersions() {
        val workDir = baseWorkDir.resolve("npm-update")

        val currentPackageJson = packageJson.get().asFile

        prepareWorkDir(
            workDir = workDir,
            currentLockFile = null,
            packageJson = currentPackageJson,
        )

        npmExec("update", "--save", workDir = workDir)

        val updatedPackageJson = workDir.resolve("package.json")
        updatedPackageJson.copyTo(currentPackageJson, overwrite = true)
    }

    /**
     * Run `npm install` in a fresh directory to generate a new `package-lock.json` file.
     *
     * Only copy the `package.json` and `package-lock.json` files to the new directory,
     * to prevent other files from interfering with the install.
     * (npm can read `yarn.lock` files, so it must not be copied.)
     */
    private fun executeNpmInstall() {
        val npmToolingProjectDir = npmToolingProjectDir.get().asFile
        val workDir = baseWorkDir.resolve("npm-install")

        val packageJson = npmToolingProjectDir.resolve("package.json")
        val currentLockFile = npmToolingProjectDir.resolve("package-lock.json")
        val updatedLockFile = workDir.resolve(currentLockFile.name)

        prepareWorkDir(
            workDir = workDir,
            currentLockFile = currentLockFile,
            packageJson = packageJson,
        )

        npmExec("install", "--package-lock-only", workDir = workDir)

        updatedLockFile.copyTo(currentLockFile, overwrite = true)
    }

    /**
     * Run `yarn install` in a fresh directory to generate a new `yarn.lock` file.
     *
     * Only copy the `package.json` and `yarn.lock` files to the new directory,
     * to prevent other files from interfering with the install.
     */
    private fun executeYarnInstall() {
        val npmToolingProjectDir = npmToolingProjectDir.get().asFile
        val workDir = baseWorkDir.resolve("yarn-install")

        val packageJson = npmToolingProjectDir.resolve("package.json")
        val currentLockFile = npmToolingProjectDir.resolve("yarn.lock")
        val updatedLockFile = workDir.resolve(currentLockFile.name)

        prepareWorkDir(
            workDir = workDir,
            currentLockFile = currentLockFile,
            packageJson = packageJson,
        )

        yarnExec("install", workDir = workDir)

        updatedLockFile.copyTo(currentLockFile, overwrite = true)
    }

    private fun prepareWorkDir(
        workDir: File,
        currentLockFile: File?,
        packageJson: File,
    ) {
        workDir.apply {
            deleteRecursively()
            mkdirs()
        }

        packageJson.copyTo(workDir.resolve(packageJson.name))

        if (currentLockFile != null && currentLockFile.exists()) {
            val updatedLockFile = workDir.resolve(currentLockFile.name)
            currentLockFile.copyTo(updatedLockFile)
        }
    }

    private fun npmExec(
        vararg args: String,
        workDir: File,
    ) {
        exec.execCapture(
            workDir = workDir,
            commandLine = buildList {
                add(nodeExecutable.get().asFile.invariantSeparatorsPath)
                add(npmCli.get().asFile.invariantSeparatorsPath)
                addAll(args)
                add("--ignore-scripts")
                add("--fund=false")
            },
        )
    }

    private fun yarnExec(
        vararg args: String,
        workDir: File,
    ) {
        exec.execCapture(
            workDir = workDir,
            commandLine = buildList {
                add(nodeExecutable.get().asFile.invariantSeparatorsPath)
                add(yarnCli.get().asFile.invariantSeparatorsPath)
                addAll(args)
                add("--non-interactive")
            },
        )
    }
}
