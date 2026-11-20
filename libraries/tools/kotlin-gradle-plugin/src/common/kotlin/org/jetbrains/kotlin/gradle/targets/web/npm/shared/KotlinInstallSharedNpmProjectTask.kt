/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm.shared

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalNodeJsToolchainDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs.NodeJsExecutable
import org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs.NodeJsRequest
import org.jetbrains.kotlin.gradle.internal.execWithProgress
import org.jetbrains.kotlin.gradle.internal.newBuildOpLogger
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.UsesKotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.targets.js.nodejs.computeNpmScriptFile
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain.isNodeJsToolchainDisabled
import org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain.isWindows
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.KotlinInstallSharedNpmProjectTask.NpmInvocation.Companion.toNpmInvocation
import org.jetbrains.kotlin.gradle.tasks.nodejs.UsesNodeJsToolchainService
import java.io.File
import javax.inject.Inject

/**
 * Installs the npm dependencies of the shared npm project of one platform,
 * assembled by [KotlinSetupSharedNpmProjectTask], into [installDirectory].
 *
 * Node.js and npm are provisioned by the Node.js toolchain service.
 *
 * If [lockFile] exists, it is used to install the exact dependencies it lists (`npm ci`).
 * Otherwise, the dependencies are resolved and a new `package-lock.json` is written to [installDirectory].
 */
@ExperimentalKotlinGradlePluginApi
@OptIn(ExperimentalNodeJsToolchainDsl::class)
@DisableCachingByDefault(because = "node_modules is large and contains symbolic links, so caching it is not beneficial")
abstract class KotlinInstallSharedNpmProjectTask
@Inject
internal constructor(
    private val objects: ObjectFactory,
    private val execOperations: ExecOperations,
) : DefaultTask(), UsesNodeJsToolchainService, UsesKotlinToolingDiagnostics {

    /**
     * The `package-lock.json` to install the dependencies from.
     */
    @get:InputFiles
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val lockFile: ConfigurableFileCollection

    /** Additional arguments for `npm install`. */
    @get:Input
    abstract val args: ListProperty<String>

    /** Whether npm lifecycle scripts of the dependencies must not be run. */
    @get:Input
    abstract val ignoreScripts: Property<Boolean>

    @get:Input
    internal abstract val nodeJsRequest: Property<NodeJsRequest>

    /** The directory the shared npm project is installed into. */
    @get:OutputDirectory
    abstract val installDirectory: DirectoryProperty

    /** The installed `node_modules` directory. */
    @get:Internal
    val nodeModulesDirectory: Provider<Directory>
        get() = installDirectory.dir(NpmProject.NODE_MODULES)

    @TaskAction
    protected fun install() {
        val lockFile = lockFile.files.firstOrNull { it.isFile } ?: return //nothing to install if there is no lock file

        val installDirectory = installDirectory.get().asFile
        lockFile.copyTo(installDirectory.resolve(PACKAGE_LOCK_JSON), overwrite = true)

        val toolchainService = nodeJsToolchainService.get()
        if (toolchainService.isNodeJsToolchainDisabled()) {
            reportDiagnostic(KotlinToolingDiagnostics.SharedNpmProjectNodeJsToolchainDisabled(path))
            return
        }

        val nodeJs = toolchainService.request(nodeJsRequest.get()).get()

        val npmArguments = buildList {
            add("install")
            addAll(args.get())
            if (logger.isDebugEnabled) add("--verbose")
            if (ignoreScripts.get()) add("--ignore-scripts")
        }

        val npm = nodeJs.toNpmInvocation()

        //from obsolete Npm.kt
        execWithProgress(objects.newBuildOpLogger(), "npm install in $installDirectory", execOps = execOperations) { execSpec ->
            execSpec.executable = npm.executable
            execSpec.args = npm.args + npmArguments
            npm.nodeBinDir?.let { nodeBinDir ->
                execSpec.environment["PATH"] = "${nodeBinDir.absolutePath}${File.pathSeparator}${System.getenv("PATH")}"
            }
            execSpec.workingDir = installDirectory
        }
    }

    /**
     * Runs the `npm-cli.js` bundled with the provisioned Node.js,
     * or `npm` from `PATH` when Node.js itself is used from `PATH`.
     */
    private class NpmInvocation(val executable: String, val args: List<String>, val nodeBinDir: File?) {
        companion object {
            internal fun NodeJsExecutable.toNpmInvocation(): NpmInvocation {
                val isWindows = platform.get().isWindows
                val nodeExecutable = executable.get()
                val nodeFile = File(nodeExecutable)
                if (nodeFile.isAbsolute) {
                    val nodeBinDir = nodeFile.parentFile
                    val nodeDir = if (isWindows) nodeBinDir else nodeBinDir.parentFile
                    val npmCli = computeNpmScriptFile(nodeDir.toPath(), "npm", isWindows).toFile()
                    if (npmCli.isFile) {
                        return NpmInvocation(nodeExecutable, listOf(npmCli.absolutePath), nodeBinDir)
                    }
                }
                return NpmInvocation(if (isWindows) "npm.cmd" else "npm", emptyList(), null)
            }
        }
    }

}

internal const val PACKAGE_LOCK_JSON = "package-lock.json"

/** The directory, relative to the project, that stores the lock files of the shared npm projects. */
internal const val KOTLIN_JS_STORE = "kotlin-js-store"
