package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.jetbrains.kotlin.gradle.targets.js.npm.NpmApiExecution
import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore
import java.io.File

data class NodeJsEnv(
    val download: Boolean,
    val cleanableStore: CleanableStore,
    val rootPackageDir: File,
    val nodeDir: File,
    val nodeBinDir: File,
    val nodeExecutable: String,

    val platformName: String,
    val architectureName: String,
    val ivyDependency: String,
    val downloadBaseUrl: String,

    val packageManager: NpmApiExecution<PackageManagerEnvironment>,
) {
    val isWindows: Boolean
        get() = platformName == "win"
}
