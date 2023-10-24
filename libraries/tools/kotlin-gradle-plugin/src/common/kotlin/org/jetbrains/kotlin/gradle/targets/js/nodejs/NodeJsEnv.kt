package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.jetbrains.kotlin.gradle.targets.js.AbstractEnv
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmApi
import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore
import java.io.File

data class NodeJsEnv(
    override val download: Boolean,
    val cleanableStore: CleanableStore,
    val rootPackageDir: File,
    override val dir: File,
    val nodeBinDir: File,
    val nodeExecutable: String,
    val platformName: String,
    val architectureName: String,
    override val ivyDependency: String,
    override val downloadBaseUrl: String?,

    val packageManager: NpmApi,
) : AbstractEnv {
    val isWindows: Boolean
        get() = platformName == "win"
}
