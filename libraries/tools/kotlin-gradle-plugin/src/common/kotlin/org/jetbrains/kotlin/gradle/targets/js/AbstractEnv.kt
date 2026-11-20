package org.jetbrains.kotlin.gradle.targets.js

import org.jetbrains.kotlin.gradle.targets.web.ObsoleteJsAndWasmToolingProvisioning
import java.io.File

@ObsoleteJsAndWasmToolingProvisioning
interface AbstractEnv {

    val download: Boolean

    val downloadBaseUrl: String?

    val allowInsecureProtocol: Boolean

    val ivyDependency: String

    val dir: File

    val executable: String
}
