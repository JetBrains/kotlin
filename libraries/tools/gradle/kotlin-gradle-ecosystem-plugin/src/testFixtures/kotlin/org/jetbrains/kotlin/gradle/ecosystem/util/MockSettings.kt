/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.ecosystem.util

import org.gradle.api.Project
import org.gradle.api.file.BuildLayout
import org.gradle.api.file.Directory
import org.gradle.api.initialization.Settings
import org.gradle.api.plugins.ExtensionAware
import org.gradle.testfixtures.ProjectBuilder
import java.lang.reflect.Proxy

/**
 * Creates a mock [Settings] instance, for testing purposes.
 */
fun mockGradleSettings(): Settings {
    val project = ProjectBuilder.builder().build()
    return mockSettings(project)
}

/**
 * [ProjectBuilder] skips the settings phase, so it cannot apply a settings plugin.
 * This [Settings] serves the services the plugin uses from [project], and fails on anything else.
 */
// Note: this is quick mock implementation. If it becomes more complex, consider using a mocking framework.
private fun mockSettings(project: Project): Settings {
    val layout = object : BuildLayout {
        override fun getSettingsDirectory(): Directory = project.layout.settingsDirectory
        override fun getRootDirectory(): Directory = project.layout.projectDirectory
    }

    val settingsExtensions = project.objects.newInstance(MockSettingsExtension::class.java)

    val settings = Proxy.newProxyInstance(Settings::class.java.classLoader, arrayOf(Settings::class.java)) { proxy, method, args ->
        when (method.name) {
            "getSettings" -> proxy
            "getExtensions" -> settingsExtensions.extensions
            "getProviders" -> project.providers
            "getGradle" -> project.gradle
            "getLayout" -> layout
            "toString" -> "fake settings"
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === args[0]
            else -> throw UnsupportedOperationException("Settings.${method.name} is not mocked")
        }
    }
    return settings as Settings
}

/**
 * Used to fetch a [org.gradle.api.plugins.ExtensionContainer] instance.
 */
private interface MockSettingsExtension : ExtensionAware
