package org.jetbrains.kotlin.gradle.plugin.experimental

import org.gradle.api.component.SoftwareComponent
import org.gradle.api.provider.Provider

interface ComponentWithBaseName: SoftwareComponent {
    fun getBaseName(): Provider<String>
}
