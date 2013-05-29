package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler

open class KotlinPluginWrapper: Plugin<Project> {
    public override fun apply(project: Project) {
        val dependencyHandler : DependencyHandler = project.getDependencies()
        dependencyHandler.create("org.jetbrains.kotlin:kotlin-gradle-plugin-core:0.1-SNAPSHOT")

    }
}