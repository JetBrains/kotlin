package org.jetbrains.kotlin.gradle.targets.js

import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore
import java.io.File

interface AbstractEnv {

    val download: Boolean

    val downloadBaseUrl: String?

    val ivyDependency: String

    val dir: File

    val executable: String

    val cleanableStore: CleanableStore
}
