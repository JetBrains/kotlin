/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.targets.js.EnvSpec
import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File

/**
 * Spec for Node.js - common JS and Wasm runtime.
 */
abstract class NodeJsEnvSpec : EnvSpec<NodeJsEnv>() {

    /**
     * Specify a platform information with name and architecture
     */
    internal abstract val platform: org.gradle.api.provider.Property<Platform>

    final override val env: Provider<NodeJsEnv> = produceEnv()

    override val executable: Provider<String> = env.map { it.executable }

    final override fun produceEnv(): Provider<NodeJsEnv> {
        return platform.map { platformValue ->
            val name = platformValue.name
            val architecture = platformValue.arch

            val versionValue = version.get()
            val nodeDirName = "node-v$versionValue-$name-$architecture"
            val cleanableStore = CleanableStore[installationDirectory.getFile().absolutePath]
            val nodeDir = cleanableStore[nodeDirName].use()
            val isWindows = platformValue.isWindows()
            val nodeBinDir = if (isWindows) nodeDir else nodeDir.resolve("bin")

            val downloadValue = download.get()
            fun getExecutable(command: String, customCommand: String, windowsExtension: String): String {
                val finalCommand =
                    if (isWindows && customCommand == command) "$command.$windowsExtension" else customCommand
                return if (downloadValue) File(nodeBinDir, finalCommand).absolutePath else finalCommand
            }

            fun getIvyDependency(): String {
                val type = if (isWindows) "zip" else "tar.gz"
                return "org.nodejs:node:$versionValue:$name-$architecture@$type"
            }

            NodeJsEnv(
                download = downloadValue,
                cleanableStore = cleanableStore,
                dir = nodeDir,
                nodeBinDir = nodeBinDir,
                executable = getExecutable("node", command.get(), "exe"),
                platformName = name,
                architectureName = architecture,
                ivyDependency = getIvyDependency(),
                downloadBaseUrl = downloadBaseUrl.orNull,
                allowInsecureProtocol = allowInsecureProtocol.get(),
            )
        }
    }

    val Project.nodeJsSetupTaskProvider: TaskProvider<out NodeJsSetupTask>
        get() = project.tasks.withType(NodeJsSetupTask::class.java).named(NodeJsSetupTask.NAME)

    companion object {
        const val EXTENSION_NAME: String = "kotlinNodeJsSpec"
    }
}
