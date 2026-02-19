package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.jetbrains.kotlin.gradle.targets.js.AbstractEnv
import java.io.File

data class NodeJsEnv(
    override val download: Boolean,
    override val dir: File,
    val nodeBinDir: File,
    override val executable: String,
    val platformName: String,
    val architectureName: String,
    override val ivyDependency: String,
    override val downloadBaseUrl: String?,
    override val allowInsecureProtocol: Boolean,
) : AbstractEnv {
    val isWindows: Boolean
        get() = platformName == "win"

    @Deprecated("Use executable instead. Scheduled for removal in Kotlin 2.3.", ReplaceWith("executable"), level = DeprecationLevel.ERROR)
    val nodeExecutable
        get() = executable
}
