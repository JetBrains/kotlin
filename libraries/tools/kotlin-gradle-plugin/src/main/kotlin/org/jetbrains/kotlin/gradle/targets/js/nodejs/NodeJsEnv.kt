package org.jetbrains.kotlin.gradle.targets.js.nodejs

import java.io.File

internal data class NodeJsEnv(
    val nodeDir: File,
    val nodeBinDir: File,
    val nodeExecutable: String,


    val platformName: String,
    val architectureName: String,
    val ivyDependency: String
) {
    val isWindows: Boolean
        get() = platformName == "win"
}
