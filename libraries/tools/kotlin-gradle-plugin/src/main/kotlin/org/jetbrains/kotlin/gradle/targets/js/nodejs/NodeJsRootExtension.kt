package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmApi
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmResolveTask
import org.jetbrains.kotlin.gradle.targets.js.yarn.Yarn
import java.io.File

open class NodeJsRootExtension(project: Project) : NodeJsExtension(project) {
    private val gradleHome = project.gradle.gradleUserHomeDir.also {
        project.logger.kotlinInfo("Storing cached files in $it")
    }

    var installationDir = gradleHome.resolve("nodejs")

    var download = true
    var nodeDownloadBaseUrl = "https://nodejs.org/dist"
    var nodeVersion = "10.15.3"

    var nodeCommand = "node"

    var packageManager: NpmApi = Yarn

    val nodeJsSetupTask: NodeJsSetupTask
        get() = project.tasks.getByName(NodeJsSetupTask.NAME) as NodeJsSetupTask

    val npmResolveTask: NpmResolveTask
        get() = project.tasks.getByName(NpmResolveTask.NAME) as NpmResolveTask

    val rootPackageDir: File
        get() = project.buildDir.resolve("js")

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

    companion object {
        const val EXTENSION_NAME: String = "kotlinNodeJs"

        operator fun get(project: Project) = NodeJsPlugin.apply(project.rootProject) as NodeJsRootExtension
    }
}
