package org.jetbrains.kotlin.gradle.targets.js

import java.io.File

interface AbstractEnv {

    val download: Boolean

    val downloadBaseUrl: String?

    val ivyDependency: String

    val dir: File
}
