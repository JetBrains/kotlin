package org.jetbrains.kotlin.gradle.targets.js.d8

import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore
import java.io.File
import java.net.URL

data class D8Env(
    val cleanableStore: CleanableStore,
    val zipPath: File,
    val targetPath: File,
    val executablePath: File,
    val isWindows: Boolean,
    val downloadUrl: URL
)