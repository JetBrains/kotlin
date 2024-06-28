package org.jetbrains.kotlin.gradle.targets.js.binaryen

import org.jetbrains.kotlin.gradle.targets.js.AbstractEnv
import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore
import java.io.File

data class BinaryenEnv(
    override val download: Boolean,
    override val downloadBaseUrl: String?,
    override val ivyDependency: String,
    override val executable: String,
    override val dir: File,
    override val cleanableStore: CleanableStore,
    val isWindows: Boolean,
) : AbstractEnv {
    val executablePath: File
        get() = File(executable)
}