package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.NpmVersions
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmApi
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependencies
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinRootNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinProjectNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinRootNpmResolver
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.targets.js.yarn.Yarn
import java.io.File

open class NodeJsRootExtension(val rootProject: Project) {
    init {
        check(rootProject.rootProject == rootProject)
    }

    private val gradleHome = rootProject.gradle.gradleUserHomeDir.also {
        rootProject.logger.kotlinInfo("Storing cached files in $it")
    }

    var installationDir = gradleHome.resolve("nodejs")

    var download = true
    var nodeDownloadBaseUrl = "https://nodejs.org/dist"
    var nodeVersion = "10.15.3"

    var nodeCommand = "node"

    var packageManager: NpmApi = Yarn

    val nodeJsSetupTask: NodeJsSetupTask
        get() = rootProject.tasks.getByName(NodeJsSetupTask.NAME) as NodeJsSetupTask

    val npmInstallTask: KotlinNpmInstallTask
        get() = rootProject.tasks.getByName(KotlinNpmInstallTask.NAME) as KotlinNpmInstallTask

    val rootPackageDir: File
        get() = rootProject.buildDir.resolve("js")

    val projectPackagesDir: File
        get() = rootPackageDir.resolve("packages")

    val nodeModulesGradleCacheDir: File
        get() = rootPackageDir.resolve("packages_imported")

    internal val environment: NodeJsEnv
        get() {
            val platform = NodeJsPlatform.name
            val architecture = NodeJsPlatform.architecture

            val nodeDir = installationDir.resolve("node-v$nodeVersion-$platform-$architecture")
            val isWindows = NodeJsPlatform.name == NodeJsPlatform.WIN
            val nodeBinDir = if (isWindows) nodeDir else nodeDir.resolve("bin")

            fun getExecutable(command: String, customCommand: String, windowsExtension: String): String {
                val finalCommand = if (isWindows && customCommand == command) "$command.$windowsExtension" else customCommand
                return if (download) File(nodeBinDir, finalCommand).absolutePath else finalCommand
            }

            fun getIvyDependency(): String {
                val type = if (isWindows) "zip" else "tar.gz"
                return "org.nodejs:node:$nodeVersion:$platform-$architecture@$type"
            }

            return NodeJsEnv(
                nodeDir = nodeDir,
                nodeBinDir = nodeBinDir,
                nodeExecutable = getExecutable("node", nodeCommand, "exe"),
                platformName = platform,
                architectureName = architecture,
                ivyDependency = getIvyDependency()
            )
        }

    internal fun executeSetup() {
        val nodeJsEnv = environment
        if (download) {
            if (!nodeJsEnv.nodeBinDir.isDirectory) {
                nodeJsSetupTask.exec()
            }
        }
    }

    val versions = NpmVersions()

    internal val resolutionState: ResolutionState
        get() = _resolutionState

    @Volatile
    private var _resolutionState: ResolutionState = ResolutionState.Resolving(KotlinRootNpmResolver(this))

    internal sealed class ResolutionState : ResolutionStateData {
        class Resolving(val resolver: KotlinRootNpmResolver) : ResolutionState(), ResolutionStateData by resolver
        class Resolved(val resolved: KotlinRootNpmResolution) : ResolutionState(), ResolutionStateData by resolved
    }

    interface ResolutionStateData {
        val compilations: Collection<KotlinJsCompilation>
    }

    internal fun requireResolver(): KotlinRootNpmResolver =
        (resolutionState as? ResolutionState.Resolving ?: error("NPM Dependencies already resolved and installed")).resolver

    internal fun resolveIfNeeded(requireUpToDateReason: String? = null): KotlinRootNpmResolution {
        val state0 = _resolutionState
        val resolution = when (state0) {
            is ResolutionState.Resolved -> state0.resolved
            is ResolutionState.Resolving -> {
                synchronized(this) {
                    val state1 = _resolutionState
                    when (state1) {
                        is ResolutionState.Resolved -> state1.resolved
                        is ResolutionState.Resolving -> state1.resolver.close().also {
                            _resolutionState = ResolutionState.Resolved(it)
                            if (requireUpToDateReason != null && !it.wasUpToDate) {
                                error("NPM dependencies should be resolved $requireUpToDateReason")
                            }
                        }
                    }
                }
            }
        }

        return resolution
    }

    internal fun requireAlreadyResolved(project: Project, reason: String = ""): KotlinProjectNpmResolution =
        resolveIfNeeded(reason)[project]

    internal fun getAlreadyResolvedOrNull(project: Project): KotlinProjectNpmResolution? {
        val state0 = resolutionState
        return when (state0) {
            is ResolutionState.Resolved -> state0.resolved[project]
            is ResolutionState.Resolving -> null
        }
    }

    internal fun checkRequiredDependencies(project: Project, target: RequiresNpmDependencies) {
        val requestedTaskDependencies = requireAlreadyResolved(project, "before $target execution").taskRequirements
        val targetRequired = requestedTaskDependencies[target]?.toSet() ?: setOf()

        target.requiredNpmDependencies.forEach {
            check(it in targetRequired) {
                "${it.createDependency(project)} required by $target was not found resolved at the time of nodejs package manager call. " +
                        "This may be caused by changing $target configuration after npm dependencies resolution."
            }
        }
    }

    companion object {
        const val EXTENSION_NAME: String = "kotlinNodeJs"
    }
}
