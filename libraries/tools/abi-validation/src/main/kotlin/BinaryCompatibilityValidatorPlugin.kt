/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.validation

import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.api.tasks.*

const val API_DIR = "api"

class BinaryCompatibilityValidatorPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        extensions.create("apiValidation", ApiValidationExtension::class.java)
        validateExtension()
        allprojects {
            configureProject(it)
        }
    }

    private fun Project.validateExtension() {
        rootProject.afterEvaluate {
            val ignored = extensions.getByType(ApiValidationExtension::class.java).ignoredProjects
            val all = allprojects.map { it.name }
            for (project in ignored) {
                require(project in all) { "Cannot find excluded project $project in all projects: $all" }
            }
        }
    }

    private fun configureProject(project: Project) {
        project.pluginManager.withPlugin("kotlin") {
            project.sourceSets.all { sourceSet ->
                if (sourceSet.name != SourceSet.MAIN_SOURCE_SET_NAME) {
                    return@all
                }
                project.configureApiTasks(sourceSet)
            }
        }

        project.pluginManager.withPlugin("kotlin-multiplatform") {
            project.sourceSets.all { sourceSet ->
                if (sourceSet.name != SourceSet.MAIN_SOURCE_SET_NAME) {
                    return@all
                }
                project.configureApiTasks(sourceSet)
            }
        }
    }
}

val Project.sourceSets: SourceSetContainer
    get() = convention.getPlugin(JavaPluginConvention::class.java).sourceSets

val Project.apiCheckEnabled
    get() = project.name !in project.rootProject.extensions.getByType(ApiValidationExtension::class.java).ignoredProjects

private fun Project.configureApiTasks(sourceSet: SourceSet) {
    val projectName = project.name
    val apiBuildDir = file(buildDir.resolve(API_DIR))
    val apiCheckDir = file(projectDir.resolve(API_DIR))

    val apiBuild = task<KotlinApiBuildTask>("apiBuild") {
        onlyIf { apiCheckEnabled }
        // 'group' is not specified deliberately so it will be hidden from ./gradlew tasks
        description =
            "Builds Kotlin API for 'main' compilations of $projectName. Complementary task and shouldn't be called manually"
        inputClassesDirs = files(provider<Any> { if (isEnabled) sourceSet.output.classesDirs else emptyList<Any>() })
        inputDependencies = files(provider<Any> { if (isEnabled) sourceSet.output.classesDirs else emptyList<Any>() })
        outputApiDir = apiBuildDir
    }

    val apiCheck = task<ApiCompareCompareTask>("apiCheck") {
        onlyIf { apiCheckEnabled }
        group = "verification"
        description = "Checks signatures of public API against the golden value in API folder for $projectName"
        expectedDir = apiCheckDir
        actualDir = apiBuildDir
        dependsOn(apiBuild)
    }

    task<Sync>("apiDump") {
        onlyIf { apiCheckEnabled }
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
