package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.api.file.DirectoryProperty
import org.jetbrains.kotlin.gradle.internal.ConfigurationPhaseAware

abstract class AbstractSettings<Env : AbstractEnv> : ConfigurationPhaseAware<Env>() {

    internal abstract val downloadProperty: org.gradle.api.provider.Property<Boolean>

    internal abstract val downloadBaseUrlProperty: org.gradle.api.provider.Property<String>

    internal abstract val installationDirectory: DirectoryProperty

    internal abstract val versionProperty: org.gradle.api.provider.Property<String>

    internal abstract val commandProperty: org.gradle.api.provider.Property<String>
}
