/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaCompilation

/**
 * Adds the kotlin-serialization-compiler-plugin-embeddable to
 * [PLUGIN_CLASSPATH_CONFIGURATION_NAME] so
 * `configureKotlinCompileTasksGradleCompatibility()` projects can use `@Serializable`.
 */
fun Project.enableKotlinSerializationPlugin() {
    addKotlinSerializationPluginDependency()
}

/**
 * Convenience overload for the compilation-scoped call sites in
 * `kotlin-gradle-plugin/build.gradle.kts`.
 */
fun KotlinWithJavaCompilation<*, *>.enableKotlinSerializationPlugin() {
    target.project.addKotlinSerializationPluginDependency()
}

private fun Project.addKotlinSerializationPluginDependency() {
    val catalogs = extensions.getByType(VersionCatalogsExtension::class.java)
    val version = catalogs.named("libs")
        .findVersion("kotlin.for.gradle.plugins.compilation")
        .get()
        .requiredVersion
    dependencies.add(
        PLUGIN_CLASSPATH_CONFIGURATION_NAME,
        "org.jetbrains.kotlin:kotlin-serialization-compiler-plugin-embeddable:$version"
    )
}
