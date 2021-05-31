/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation

import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import java.io.*

const val API_DIR = "api"

class BinaryCompatibilityValidatorPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        val extension = extensions.create("apiValidation", ApiValidationExtension::class.java)
        validateExtension(extension)
        allprojects {
            configureProject(it, extension)
        }
    }

    private fun Project.validateExtension(extension: ApiValidationExtension) {
        afterEvaluate {
            val ignored = extension.ignoredProjects
            val all = allprojects.map { it.name }
            for (project in ignored) {
                require(project in all) { "Cannot find excluded project $project in all projects: $all" }
            }
        }
    }

    private fun configureProject(project: Project, extension: ApiValidationExtension) {
        project.pluginManager.withPlugin("kotlin") {
            if (project.name in extension.ignoredProjects) return@withPlugin
            project.sourceSets.all { sourceSet ->
                if (sourceSet.name != SourceSet.MAIN_SOURCE_SET_NAME) {
                    return@all
                }
                project.configureApiTasks(sourceSet, extension)
            }
        }

        project.pluginManager.withPlugin("kotlin-android") {
            if (project.name in extension.ignoredProjects) return@withPlugin
            val androidExtension = project.extensions.getByName("kotlin") as KotlinAndroidProjectExtension
            androidExtension.target.compilations.matching {
                it.compilationName == "release"
            }.all {
                project.configureKotlinCompilation(it, extension, useOutput = true)
            }
        }

        project.pluginManager.withPlugin("kotlin-multiplatform") {
            if (project.name in extension.ignoredProjects) return@withPlugin
            val kotlin = project.extensions.getByName("kotlin") as KotlinMultiplatformExtension
            kotlin.targets.matching {
                it.platformType == KotlinPlatformType.jvm || it.platformType == KotlinPlatformType.androidJvm
            }.all { target ->
                if (target.platformType == KotlinPlatformType.jvm) {
                    target.compilations.matching { it.name == "main" }.all {
                        project.configureKotlinCompilation(it, extension)
                    }
                } else if (target.platformType == KotlinPlatformType.androidJvm) {
                    target.compilations.matching { it.name == "release" }.all {
                        project.configureKotlinCompilation(it, extension, useOutput = true)
                    }
                }
            }
        }
    }
}

private fun Project.configureKotlinCompilation(
    compilation: KotlinCompilation<KotlinCommonOptions>,
    extension: ApiValidationExtension,
    useOutput: Boolean = false
) {
    val projectName = project.name
    val apiBuildDir = file(buildDir.resolve(API_DIR))
    val apiBuild = task<KotlinApiBuildTask>("apiBuild", extension) {
        // Do not enable task for empty umbrella modules
        isEnabled = apiCheckEnabled(extension) && compilation.allKotlinSourceSets.any { it.kotlin.srcDirs.any { it.exists() } }
        // 'group' is not specified deliberately so it will be hidden from ./gradlew tasks
        description =
            "Builds Kotlin API for 'main' compilations of $projectName. Complementary task and shouldn't be called manually"
        if (useOutput) {
            // Workaround for #4
            inputClassesDirs = files(provider<Any> { if (isEnabled) compilation.output.classesDirs else emptyList<Any>() })
            inputDependencies = files(provider<Any> { if (isEnabled) compilation.output.classesDirs else emptyList<Any>() })
        } else {
            inputClassesDirs = files(provider<Any> { if (isEnabled) compilation.output.classesDirs else emptyList<Any>() })
            inputDependencies = files(provider<Any> { if (isEnabled) compilation.compileDependencyFiles else emptyList<Any>() })
        }
        outputApiDir = apiBuildDir
    }
    configureCheckTasks(apiBuildDir, apiBuild, extension)
}

val Project.sourceSets: SourceSetContainer
    get() = convention.getPlugin(JavaPluginConvention::class.java).sourceSets

fun Project.apiCheckEnabled(extension: ApiValidationExtension): Boolean =
    name !in extension.ignoredProjects && !extension.validationDisabled

private fun Project.configureApiTasks(sourceSet: SourceSet, extension: ApiValidationExtension) {
    val projectName = project.name
    val apiBuildDir = file(buildDir.resolve(API_DIR))
    val apiBuild = task<KotlinApiBuildTask>("apiBuild", extension) {
        isEnabled = apiCheckEnabled(extension)
        // 'group' is not specified deliberately so it will be hidden from ./gradlew tasks
        description =
            "Builds Kotlin API for 'main' compilations of $projectName. Complementary task and shouldn't be called manually"
        inputClassesDirs = files(provider<Any> { if (isEnabled) sourceSet.output.classesDirs else emptyList<Any>() })
        inputDependencies = files(provider<Any> { if (isEnabled) sourceSet.output.classesDirs else emptyList<Any>() })
        outputApiDir = apiBuildDir
    }

    configureCheckTasks(apiBuildDir, apiBuild, extension)
}

private fun Project.configureCheckTasks(
    apiBuildDir: File,
    apiBuild: TaskProvider<KotlinApiBuildTask>,
    extension: ApiValidationExtension
) {
    val projectName = project.name
    val apiCheckDir = file(projectDir.resolve(API_DIR))
    val apiCheck = task<ApiCompareCompareTask>("apiCheck") {
        isEnabled = apiCheckEnabled(extension) && apiBuild.map { it.enabled }.getOrElse(true)
        group = "verification"
        description = "Checks signatures of public API against the golden value in API folder for $projectName"
        projectApiDir = if (apiCheckDir.exists()) {
            apiCheckDir
        } else {
            nonExistingProjectApiDir = apiCheckDir.toString()
            null
        }
        this.apiBuildDir = apiBuildDir
        dependsOn(apiBuild)
    }

    task<Sync>("apiDump") {
        isEnabled = apiCheckEnabled(extension) && apiBuild.map { it.enabled }.getOrElse(true)
        group = "other"
        description = "Syncs API from build dir to $API_DIR dir for $projectName"
        from(apiBuildDir)
        into(apiCheckDir)
        dependsOn(apiBuild)
        doFirst {
            apiCheckDir.mkdirs()
        }
    }
    project.tasks.getByName("check").dependsOn(apiCheck)
}

inline fun <reified T : Task> Project.task(
    name: String,
    noinline configuration: T.() -> Unit
): TaskProvider<T> = tasks.register(name, T::class.java, Action(configuration))

inline fun <reified T : Task> Project.task(
    name: String,
    extension: ApiValidationExtension,
    noinline configuration: T.() -> Unit
): TaskProvider<T> = tasks.register(name, T::class.java, extension).also {
    it.configure(Action(configuration))
}
