package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.logging.kotlinWarn
import org.jetbrains.kotlin.gradle.utils.ParsedGradleVersion
import java.util.*

object CopyClassesToJavaOutputStatus {
    fun isEnabled(project: Project): Boolean {
        val kotlinJvmExt = project.kotlinJvmExt ?: return false
        if (kotlinJvmExt.copyClassesToJavaOutput) {
            if (isGradleVersionTooLow(project)) {
                if (warningReportedForProject.add(project)) {
                    project.logger.kotlinWarn(gradleVersionTooLowWarningMessage)
                }
            } else {
                addCacheWarningToJavaTasks(project)
            }
            return true
        }
        return false
    }

    private val warningReportedForProject = Collections.newSetFromMap<Project>(WeakHashMap())

    private fun isGradleVersionTooLow(project: Project): Boolean {
        return (ParsedGradleVersion.parse(project.gradle.gradleVersion) ?: return false) < ParsedGradleVersion(4, 0)
    }

    private fun addCacheWarningToJavaTasks(project: Project) {
        project.tasks.withType(JavaCompile::class.java).all {
            val warningCacheIfSpec = Spec<Task> {
                if (warningReportedForProject.add(project)) {
                    project.logger.kotlinWarn(buildCacheWarningMessage)
                }
                return@Spec true
            }
            it.outputs.javaClass.getMethod("cacheIf", Spec::class.java).invoke(it.outputs, warningCacheIfSpec)
        }
    }

    const val buildCacheWarningMessage = "The 'kotlin.copyClassesToJavaOutput' option should not be used with Gradle " +
            "build cache"

    const val gradleVersionTooLowWarningMessage = "The 'kotlin.copyClassesToJavaOutput' option has no effect when " +
            "used with Gradle < 4.0"

    private val Project.kotlinJvmExt: KotlinJvmProjectExtension?
        get() = extensions.findByType(KotlinJvmProjectExtension::class.java)
}