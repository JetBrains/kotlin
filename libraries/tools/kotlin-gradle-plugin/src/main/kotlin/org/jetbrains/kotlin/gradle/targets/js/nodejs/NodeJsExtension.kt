package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import java.io.File

open class NodeJsExtension(project: Project) {
    private val gradleHome = project.gradle.gradleUserHomeDir.also {
        project.logger.kotlinInfo("Storing cached files in $it")
    }

    var installationDir = gradleHome.resolve("nodejs")

    var distBaseUrl = "https://nodejs.org/dist"
    var version = "10.15.3"
    var npmVersion = ""

    var nodeCommand = "node"
    var npmCommand = "npm"

    var download = true

    internal fun buildEnv(): NodeJsEnv {
        val platform = NodeJsPlatform.name
        val architecture = NodeJsPlatform.architecture

        val nodeDir = installationDir.resolve("node-v$version-$platform-$architecture")
        val isWindows = NodeJsPlatform.name == NodeJsPlatform.WIN
        val nodeBinDir = if (isWindows) nodeDir else nodeDir.resolve("bin")

        fun getExecutable(command: String, customCommand: String, windowsExtension: String): String {
            val finalCommand = if (isWindows && customCommand == command) "$command.$windowsExtension" else customCommand
            return if (download) File(nodeBinDir, finalCommand).absolutePath else finalCommand
        }

        fun getIvyDependency(): String {
            val type = if (isWindows) "zip" else "tar.gz"
            return "org.nodejs:node:$version:$platform-$architecture@$type"
        }

        return NodeJsEnv(
                nodeDir = nodeDir,
                nodeBinDir = nodeBinDir,
                nodeExec = getExecutable("node", nodeCommand, "exe"),
                npmExec = getExecutable("npm", npmCommand, "cmd"),
                platformName = platform,
                architectureName = architecture,
                ivyDependency = getIvyDependency()
        )
    }

    companion object {
        const val NODE_JS: String = "nodeJs"

        operator fun get(project: Project): NodeJsExtension {
            val extension = project.extensions.findByType(NodeJsExtension::class.java)
            if (extension != null)
                return extension

            val parentProject = project.parent
            if (parentProject != null)
                return get(parentProject)

            throw GradleException("NodeJsExtension is not installed")
        }
    }
}
