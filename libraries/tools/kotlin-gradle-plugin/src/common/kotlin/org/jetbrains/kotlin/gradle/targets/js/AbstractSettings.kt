package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.api.file.DirectoryProperty
import org.jetbrains.kotlin.gradle.internal.ConfigurationPhaseAware
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File

abstract class AbstractSettings<Env : AbstractEnv> : ConfigurationPhaseAware<Env>() {

    @Deprecated("This property has been migrated to support the Provider API. Use corresponding spec (extension with name *Spec) instead. This will be removed in version 2.2.")
    var download: Boolean
        get() = downloadProperty.get()
        set(value) {
            downloadProperty.set(value)
        }

    internal abstract val downloadProperty: org.gradle.api.provider.Property<Boolean>

    @Deprecated("This property has been migrated to support the Provider API. Use downloadBaseUrlProperty instead. This will be removed in version 2.2.")
    var downloadBaseUrl: String?
        get() = downloadBaseUrlProperty.getOrNull()
        set(value) {
            downloadBaseUrlProperty.set(value)
        }

    internal abstract val downloadBaseUrlProperty: org.gradle.api.provider.Property<String>

    @Deprecated("This property has been migrated to support the Provider API. Use corresponding spec (extension with name *Spec) instead. This will be removed in version 2.2.")
    var installationDir: File
        get() = installationDirectory.getFile()
        set(value) {
            installationDirectory.fileValue(value)
        }

    internal abstract val installationDirectory: DirectoryProperty

    @Deprecated("This property has been migrated to support the Provider API. Use corresponding spec (extension with name *Spec) instead. This will be removed in version 2.2.")
    var version: String
        get() = versionProperty.get()
        set(value) {
            versionProperty.set(value)
        }

    internal abstract val versionProperty: org.gradle.api.provider.Property<String>

    @Deprecated("This property has been migrated to support the Provider API. Use corresponding spec (extension with name *Spec) instead. This will be removed in version 2.2.")
    var command: String
        get() = commandProperty.get()
        set(value) {
            commandProperty.set(value)
        }

    internal abstract val commandProperty: org.gradle.api.provider.Property<String>
}
