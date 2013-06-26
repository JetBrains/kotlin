package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.specs.Spec
import java.io.File
import java.net.URL
import org.gradle.api.logging.Logging
import java.util.Properties
import java.io.FileNotFoundException

open class KotlinAndroidPlugin: Plugin<Project> {

    val log = Logging.getLogger(getClass())!!

    public override fun apply(project: Project) {

    }
}

open class KSpec<T: Any?>(val predicate: (T) -> Boolean): Spec<T> {
    public override fun isSatisfiedBy(p0: T?): Boolean {
        return p0 != null && predicate(p0)
    }
}
