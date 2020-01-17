package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.targets.js.NpmVersions
import org.jetbrains.kotlin.gradle.targets.js.npm.KotlinNpmResolutionManager
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmApi
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.targets.js.yarn.Yarn
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.utils.deprecatedBecauseNoConfigAvoidanceUseProvider
import org.jetbrains.kotlin.gradle.utils.warnAccessToDeprecatedNoConfigAvoidanceSymbol
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

    private val projectProperties = PropertiesProvider(rootProject)

    inner class Experimental {
        val generateKotlinExternals: Boolean
            get() = projectProperties.jsGenerateExternals == true

        val discoverTypes: Boolean
            get() = projectProperties.jsDiscoverTypes == true
    }

    val experimental = Experimental()

    val npmInstallTaskProvider: TaskProvider<KotlinNpmInstallTask>
        get() = rootProject.locateTask(KotlinNpmInstallTask.NAME)!!

    val nodeJsSetupTaskProvider: TaskProvider<NodeJsSetupTask>
        get() = rootProject.locateTask(NodeJsSetupTask.NAME)!!

    @Deprecated(deprecatedBecauseNoConfigAvoidanceUseProvider, ReplaceWith("nodeJsSetupTaskProvider"))
    val nodeJsSetupTask: NodeJsSetupTask
        get() = nodeJsSetupTaskProvider.get().also {
            rootProject.logger.warnAccessToDeprecatedNoConfigAvoidanceSymbol("nodeJsSetupTask")
        }

    @Deprecated(deprecatedBecauseNoConfigAvoidanceUseProvider, ReplaceWith("npmInstallTaskProvider"))
    val npmInstallTask: KotlinNpmInstallTask
        get() = npmInstallTaskProvider.get().also {
            rootProject.logger.warnAccessToDeprecatedNoConfigAvoidanceSymbol("npmInstallTask")
        }

    val rootPackageDir: File
        get() = rootProject.buildDir.resolve("js")

    internal val rootNodeModulesStateFile: File
        get() = rootPackageDir.resolve("node_modules.state")

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
                nodeJsSetupTaskProvider.get().exec()
            }
        }
    }

    val versions = NpmVersions()
    internal val npmResolutionManager = KotlinNpmResolutionManager(this)

    companion object {
        const val EXTENSION_NAME: String = "kotlinNodeJs"
    }
}
