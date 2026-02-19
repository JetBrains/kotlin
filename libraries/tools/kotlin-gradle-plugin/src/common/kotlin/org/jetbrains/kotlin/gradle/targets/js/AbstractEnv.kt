package org.jetbrains.kotlin.gradle.targets.js

import java.io.File

interface AbstractEnv {

    val download: Boolean

    val downloadBaseUrl: String?

    val allowInsecureProtocol: Boolean

    val ivyDependency: String

    val dir: File

    val executable: String
}
