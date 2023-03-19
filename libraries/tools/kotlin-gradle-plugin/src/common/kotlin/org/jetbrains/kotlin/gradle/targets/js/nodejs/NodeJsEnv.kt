package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.jetbrains.kotlin.gradle.targets.js.npm.NpmApi
import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore
import java.io.File

data class NodeJsEnv(
    val cleanableStore: CleanableStore,
    val rootPackageDir: File,
    val nodeDir: File,
    val nodeBinDir: File,
    val nodeExecutable: String,

    val platformName: String,
    val architectureName: String,
    val ivyDependency: String,
    val downloadBaseUrl: String,

    val packageManager: NpmApi,
) {
    val isWindows: Boolean
        get() = platformName == "win"
}
