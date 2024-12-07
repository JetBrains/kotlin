package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.jetbrains.kotlin.gradle.targets.js.AbstractEnv
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmApiExecution
import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore
import java.io.File

data class NodeJsEnv(
    override val download: Boolean,
    override val cleanableStore: CleanableStore,
    override val dir: File,
    val nodeBinDir: File,
    override val executable: String,
    val platformName: String,
    val architectureName: String,
    override val ivyDependency: String,
    override val downloadBaseUrl: String?,
) : AbstractEnv {
    val isWindows: Boolean
        get() = platformName == "win"

    @Deprecated("Use executable instead", ReplaceWith("executable"))
    val nodeExecutable
        get() = executable
}
