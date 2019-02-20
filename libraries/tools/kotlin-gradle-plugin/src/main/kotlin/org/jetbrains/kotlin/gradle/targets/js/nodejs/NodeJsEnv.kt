package org.jetbrains.kotlin.gradle.targets.js.nodejs

import java.io.*

internal data class NodeJsEnv(
        val nodeDir: File,
        val nodeBinDir: File,
        val nodeExec: String,
        val npmExec: String,

        val platformName: String,
        val architectureName: String,
        val ivyDependency: String
) {
    val isWindows: Boolean
        get() = platformName == "win"
}
