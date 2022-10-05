/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation

import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.api.provider.*
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
        configureKotlinPlugin(project, extension)
        configureAndroidPlugin(project, extension)
        configureMultiplatformPlugin(project, extension)
    }

    private fun configurePlugin(
        name: String,
        project: Project,
        extension: ApiValidationExtension,
        action: Action<AppliedPlugin>
    ) = project.pluginManager.withPlugin(name) {
        if (project.name in extension.ignoredProjects) return@withPlugin
        action.execute(it)
    }

    private fun configureMultiplatformPlugin(
        project: Project,
        extension: ApiValidationExtension
    ) = configurePlugin("kotlin-multiplatform", project, extension) {
        if (project.name in extension.ignoredProjects) return@configurePlugin
        val kotlin = project.extensions.getByName("kotlin") as KotlinMultiplatformExtension

        // Create common tasks for multiplatform
        val commonApiDump = project.tasks.register("apiDump") {
            it.group = "other"
            it.description = "Task that collects all target specific dump tasks"
        }

        val commonApiCheck: TaskProvider<Task> = project.tasks.register("apiCheck") {
            it.group = "verification"
            it.description = "Shortcut task that depends on all specific check tasks"
        }.apply { project.tasks.named("check") { it.dependsOn(this) } }

        val jvmTargetCountProvider = project.provider {
            kotlin.targets.count {
                it.platformType in arrayOf(
                    KotlinPlatformType.jvm,
                    KotlinPlatformType.androidJvm
                )
            }
        }

        val dirConfig = jvmTargetCountProvider.map {
            if (it == 1) DirConfig.COMMON else DirConfig.TARGET_DIR
        }

        kotlin.targets.matching {
            it.platformType == KotlinPlatformType.jvm || it.platformType == KotlinPlatformType.androidJvm
        }.all { target ->
            val targetConfig = TargetConfig(project, target.name, dirConfig)
            if (target.platformType == KotlinPlatformType.jvm) {
                target.compilations.matching { it.name == "main" }.all {
                    project.configureKotlinCompilation(it, extension, targetConfig, commonApiDump, commonApiCheck)
                }
            } else if (target.platformType == KotlinPlatformType.androidJvm) {
                target.compilations.matching { it.name == "release" }.all {
                    project.configureKotlinCompilation(
                        it,
                        extension,
                        targetConfig,
                        commonApiDump,
                        commonApiCheck,
                        useOutput = true
                    )
                }
            }
        }
    }

    private fun configureAndroidPlugin(
        project: Project,
        extension: ApiValidationExtension
    ) = configurePlugin("kotlin-android", project, extension) {
        val androidExtension = project.extensions.getByName("kotlin") as KotlinAndroidProjectExtension
        androidExtension.target.compilations.matching {
            it.compilationName == "release"
        }.all {
            project.configureKotlinCompilation(it, extension, useOutput = true)
        }
    }

    private fun configureKotlinPlugin(
        project: Project,
        extension: ApiValidationExtension
    ) = configurePlugin("kotlin", project, extension) {
        project.sourceSets.all { sourceSet ->
            if (sourceSet.name != SourceSet.MAIN_SOURCE_SET_NAME) {
                return@all
            }
            project.configureApiTasks(sourceSet, extension, TargetConfig(project))
        }
    }
}

private class TargetConfig constructor(
    project: Project,
    val targetName: String? = null,
    private val dirConfig: Provider<DirConfig>? = null,
) {

    private val API_DIR_PROVIDER = project.provider { API_DIR }

    fun apiTaskName(suffix: String) = when (targetName) {
        null, "" -> "api$suffix"
        else     -> "${targetName}Api$suffix"
    }

    val apiDir
        get() = dirConfig?.map { dirConfig ->
            when (dirConfig) {
                DirConfig.COMMON -> API_DIR
                else -> "$API_DIR/$targetName"
            }
        } ?: API_DIR_PROVIDER
}

private enum class DirConfig {
    /**
     * `api` directory for .api files.
     * Used in single target projects
     */
    COMMON,
    /**
     * Target-based directory, used in multitarget setups.
     * E.g. for the project with targets jvm and android,
     * the resulting paths will be
     * `/api/jvm/project.api` and `/api/android/project.api`
     */
    TARGET_DIR,
}

private fun Project.configureKotlinCompilation(
    compilation: KotlinCompilation<KotlinCommonOptions>,
    extension: ApiValidationExtension,
    targetConfig: TargetConfig = TargetConfig(this),
    commonApiDump: TaskProvider<Task>? = null,
    commonApiCheck: TaskProvider<Task>? = null,
    useOutput: Boolean = false,
) {
    val projectName = project.name
    val apiDirProvider = targetConfig.apiDir
    val apiBuildDir = apiDirProvider.map { buildDir.resolve(it) }

    val apiBuild = task<KotlinApiBuildTask>(targetConfig.apiTaskName("Build")) {
        // Do not enable task for empty umbrella modules
        isEnabled =
            apiCheckEnabled(projectName, extension) && compilation.allKotlinSourceSets.any { it.kotlin.srcDirs.any { it.exists() } }
        // 'group' is not specified deliberately, so it will be hidden from ./gradlew tasks
        description =
            "Builds Kotlin API for 'main' compilations of $projectName. Complementary task and shouldn't be called manually"
        if (useOutput) {
            // Workaround for #4
            inputClassesDirs =
                files(provider<Any> { if (isEnabled) compilation.output.classesDirs else emptyList<Any>() })
            inputDependencies =
                files(provider<Any> { if (isEnabled) compilation.output.classesDirs else emptyList<Any>() })
        } else {
            inputClassesDirs =
                files(provider<Any> { if (isEnabled) compilation.output.classesDirs else emptyList<Any>() })
            inputDependencies =
                files(provider<Any> { if (isEnabled) compilation.compileDependencyFiles else emptyList<Any>() })
        }
        outputApiDir = apiBuildDir.get()
    }
    configureCheckTasks(apiBuildDir, apiBuild, extension, targetConfig, commonApiDump, commonApiCheck)
}

val Project.sourceSets: SourceSetContainer
    get() = convention.getPlugin(JavaPluginConvention::class.java).sourceSets

internal val Project.apiValidationExtensionOrNull: ApiValidationExtension?
    get() =
        generateSequence(this) { it.parent }
            .map { it.extensions.findByType(ApiValidationExtension::class.java) }
            .firstOrNull { it != null }

fun apiCheckEnabled(projectName: String, extension: ApiValidationExtension): Boolean =
    projectName !in extension.ignoredProjects && !extension.validationDisabled

private fun Project.configureApiTasks(
    sourceSet: SourceSet,
    extension: ApiValidationExtension,
    targetConfig: TargetConfig = TargetConfig(this),
) {
    val projectName = project.name
    val apiBuildDir = targetConfig.apiDir.map { buildDir.resolve(it) }
    val apiBuild = task<KotlinApiBuildTask>(targetConfig.apiTaskName("Build")) {
        isEnabled = apiCheckEnabled(projectName, extension)
        // 'group' is not specified deliberately so it will be hidden from ./gradlew tasks
        description =
            "Builds Kotlin API for 'main' compilations of $projectName. Complementary task and shouldn't be called manually"
        inputClassesDirs = files(provider<Any> { if (isEnabled) sourceSet.output.classesDirs else emptyList<Any>() })
        inputDependencies = files(provider<Any> { if (isEnabled) sourceSet.output.classesDirs else emptyList<Any>() })
        outputApiDir = apiBuildDir.get()
    }

    configureCheckTasks(apiBuildDir, apiBuild, extension, targetConfig)
}

private fun Project.configureCheckTasks(
    apiBuildDir: Provider<File>,
    apiBuild: TaskProvider<KotlinApiBuildTask>,
    extension: ApiValidationExtension,
    targetConfig: TargetConfig,
    commonApiDump: TaskProvider<Task>? = null,
    commonApiCheck: TaskProvider<Task>? = null,
) {
    val projectName = project.name
    val apiCheckDir = targetConfig.apiDir.map {
        projectDir.resolve(it).also { r ->
            logger.debug("Configuring api for ${targetConfig.targetName ?: "jvm"} to $r")
        }
    }
    val apiCheck = task<KotlinApiCompareTask>(targetConfig.apiTaskName("Check")) {
        isEnabled = apiCheckEnabled(projectName, extension) && apiBuild.map { it.enabled }.getOrElse(true)
        group = "verification"
        description = "Checks signatures of public API against the golden value in API folder for $projectName"
        compareApiDumps(apiReferenceDir = apiCheckDir.get(), apiBuildDir = apiBuildDir.get())
        dependsOn(apiBuild)
    }

    val apiDump = task<Sync>(targetConfig.apiTaskName("Dump")) {
        isEnabled = apiCheckEnabled(projectName, extension) && apiBuild.map { it.enabled }.getOrElse(true)
        group = "other"
        description = "Syncs API from build dir to ${targetConfig.apiDir} dir for $projectName"
        from(apiBuildDir)
        into(apiCheckDir)
        dependsOn(apiBuild)
    }

    commonApiDump?.configure { it.dependsOn(apiDump) }

    when (commonApiCheck) {
        null -> project.tasks.named("check").configure { it.dependsOn(apiCheck) }
        else -> commonApiCheck.configure { it.dependsOn(apiCheck) }
    }
}

inline fun <reified T : Task> Project.task(
    name: String,
    noinline configuration: T.() -> Unit,
): TaskProvider<T> = tasks.register(name, T::class.java, Action(configuration))

inline fun <reified T : Task> Project.task(
    name: String,
    extension: ApiValidationExtension,
    noinline configuration: T.() -> Unit,
): TaskProvider<T> = tasks.register(name, T::class.java, extension).also {
    it.configure(Action(configuration))
}
