package org.jetbrains.kotlin.gradle.targets.js

import org.jetbrains.kotlin.gradle.internal.ConfigurationPhaseAware
import java.io.File

abstract class AbstractSettings<Env : AbstractEnv> : ConfigurationPhaseAware<Env>() {

    abstract var download: Boolean

    abstract var downloadBaseUrl: String?

    abstract var installationDir: File

    abstract var version: String

    abstract var command: String
}
