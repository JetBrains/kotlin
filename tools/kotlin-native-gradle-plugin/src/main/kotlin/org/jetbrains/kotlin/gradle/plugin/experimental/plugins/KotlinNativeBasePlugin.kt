package org.jetbrains.kotlin.gradle.plugin.experimental.plugins

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.component.SoftwareComponentContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.internal.FeaturePreviews
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.TaskContainer
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.language.plugins.NativeBasePlugin
import org.gradle.nativeplatform.test.tasks.RunTestExecutable
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.KonanPlugin
import org.jetbrains.kotlin.gradle.plugin.experimental.internal.*
import org.jetbrains.kotlin.gradle.plugin.experimental.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.plugin.hasProperty
import org.jetbrains.kotlin.gradle.plugin.konanCompilerDownloadDir
import org.jetbrains.kotlin.gradle.plugin.setProperty
import org.jetbrains.kotlin.gradle.plugin.tasks.KonanCompilerDownloadTask
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget

class KotlinNativeBasePlugin: Plugin<ProjectInternal> {

    private fun TaskContainer.createRunTestTask(
            taskName: String,
            binary: KotlinNativeTestExecutableImpl,
            compileTask: KotlinNativeCompile
    ) = create(taskName, RunTestExecutable::class.java).apply {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = "Executes Kotlin/Native unit tests."

            val testExecutableProperty = compileTask.outputFile
            executable = testExecutableProperty.asFile.get().absolutePath

            onlyIf { testExecutableProperty.asFile.get().exists() }
            inputs.file(testExecutableProperty)
            dependsOn(testExecutableProperty)

            // TODO: Find or implement some mechanism for test result saving.
            outputDir = project.layout.buildDirectory.dir("test-results/" + binary.names.dirName).get().asFile
        }

    private fun TaskContainer.createDummyTestTask(taskName: String, target: KonanTarget) =
            create(taskName, DefaultTask::class.java).apply {
                doLast {
                    logger.warn("""

                        The current host doesn't support running executables for target '${target.name}'.
                        Skipping tests.

                    """.trimIndent())
                }
            }

    private val KonanTarget.canRunOnHost: Boolean
        get() = this == HostManager.host

    private fun addCompilationTasks(
            tasks: TaskContainer,
            components: SoftwareComponentContainer,
            buildDirectory: DirectoryProperty,
            providers: ProviderFactory
    ) {
        components.withType(AbstractKotlinNativeBinary::class.java) { binary ->
            val names = binary.names
            val target = binary.konanTarget
            val kind = binary.kind

            val compileTask = tasks.create(
                    names.getCompileTaskName(LANGUAGE_NAME),
                    KotlinNativeCompile::class.java
            ).apply {
                this.binary = binary
                outputFile.set(buildDirectory.file(providers.provider {
                    val root = binary.outputRootName
                    val prefix = kind.prefix(target)
                    val suffix = kind.suffix(target)
                    val baseName = binary.getBaseName().get()
                    "$root/${names.dirName}/${prefix}${baseName}${suffix}"
                }))

                group = BasePlugin.BUILD_GROUP
                description = "Compiles Kotlin/Native source set '${binary.sourceSet.name}' into a ${binary.kind.name.toLowerCase()}"
            }
            binary.compileTask.set(compileTask)
            binary.outputs.from(compileTask.outputFile)

            when(binary) {
                is KotlinNativeExecutableImpl -> binary.runtimeFile.set(compileTask.outputFile)
                is AbstractKotlinNativeLibrary -> binary.linkFile.set(compileTask.outputFile)
            }

            if (binary is KotlinNativeTestExecutableImpl) {
                val taskName = binary.names.getTaskName("run")
                val testTask = if (target.canRunOnHost) {
                    tasks.createRunTestTask(taskName, binary, compileTask)
                } else {
                    // We create dummy tasks that just write an error message to avoid having an unset
                    // runTask property for targets that cannot be executed on the current host.
                    tasks.createDummyTestTask(taskName, target)
                }
                binary.runTask.set(testTask)
            }
        }
    }

    private fun ProjectInternal.checkGradleMetadataFeature() {
        val metadataEnabled = gradle.services
            .get(FeaturePreviews::class.java)
            .isFeatureEnabled(FeaturePreviews.Feature.GRADLE_METADATA)
        if (!metadataEnabled) {
            logger.warn(
                "The GRADLE_METADATA feature is not enabled: publication and external dependencies will not work properly."
            )
        }
    }

    private fun checkGradleVersion() =  GradleVersion.current().let { current ->
        check(current >= KonanPlugin.REQUIRED_GRADLE_VERSION) {
            "Kotlin/Native Gradle plugin is incompatible with this version of Gradle.\n" +
                    "The minimal required version is ${KonanPlugin.REQUIRED_GRADLE_VERSION}\n" +
                    "Current version is ${current}"
        }
    }

    private fun ProjectInternal.addCompilerDownloadingTask(): KonanCompilerDownloadTask {
        val result = tasks.create(KonanPlugin.KONAN_DOWNLOAD_TASK_NAME, KonanCompilerDownloadTask::class.java)
        if (!hasProperty(KonanPlugin.ProjectProperty.KONAN_HOME)) {
            setProperty(KonanPlugin.ProjectProperty.KONAN_HOME, project.konanCompilerDownloadDir())
            setProperty(KonanPlugin.ProjectProperty.DOWNLOAD_COMPILER, true)
        }
        return result
    }

    override fun apply(project: ProjectInternal): Unit = with(project) {
        // TODO: Deal with compiler downloading.
        // Apply base plugins
        project.pluginManager.apply(LifecycleBasePlugin::class.java)
        project.pluginManager.apply(NativeBasePlugin::class.java)

        checkGradleMetadataFeature()
        checkGradleVersion()
        addCompilerDownloadingTask()

        // Create compile tasks
        addCompilationTasks(tasks, components, layout.buildDirectory, providers)
    }

    companion object {
        const val LANGUAGE_NAME = "KotlinNative"
        const val SOURCE_SETS_EXTENSION = "sourceSets"
    }

}
