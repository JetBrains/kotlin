package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project

/**
 * Generic action to configure/set up a Kotlin Gradle Project.
 * Will be invoked after the KotlinPluginWrapper applied the underlying Kotlin Gradle Plugin
 */
internal fun interface KotlinProjectSetupAction {
    fun Project.invoke()

    companion object {
        val extensionPoint = KotlinGradlePluginExtensionPoint<KotlinProjectSetupAction>()
    }
}

internal fun Project.runKotlinProjectSetupActions() {
    KotlinProjectSetupAction.extensionPoint[this].forEach { action ->
        with(action) { invoke() }
    }
}
