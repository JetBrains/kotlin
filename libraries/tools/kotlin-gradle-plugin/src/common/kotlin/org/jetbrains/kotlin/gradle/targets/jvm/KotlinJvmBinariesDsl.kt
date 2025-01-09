/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.jvm

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ApplicationPlugin.APPLICATION_GROUP
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.jvm.toolchain.JavaToolchainService
import org.jetbrains.kotlin.gradle.dsl.KotlinGradlePluginDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.internal.compatibilityWrapper
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.utils.newInstance
import javax.inject.Inject

// Should be as much close to Gradle 'JavaApplication' spec as possible
@KotlinGradlePluginDsl
interface KotlinJvmBinaryDsl {
    /**
     * The name of the application's Java module if it should run as a module.
     */
    val mainModule: Property<String>

    /**
     * The fully qualified name of the application's main class.
     */
    val mainClass: Property<String>

    /**
     * The name of the application.
     */
    val applicationName: Property<String>

    /**
     * Array of string arguments to pass to the JVM when running the application.
     */
    val applicationDefaultJvmArgs: ListProperty<String>

    /**
     * Directory to place executables in.
     */
    val executableDir: DirectoryProperty
}

@KotlinGradlePluginDsl
interface KotlinJvmBinariesDsl {
    /**
     * Creates [JavaExec] task to run configured in the [KotlinJvmBinariesDsl] spec class from this target
     * main compilation.
     */
    fun executable(
        configure: KotlinJvmBinaryDsl.() -> Unit
    ): TaskProvider<JavaExec> = executable(KotlinCompilation.MAIN_COMPILATION_NAME, configure)

    /**
     * Creates [JavaExec] task to run configured in the [KotlinJvmBinariesDsl] spec class from this target
     * main compilation.
     */
    fun executable(
        configure: Action<KotlinJvmBinaryDsl>
    ): TaskProvider<JavaExec> = executable(KotlinCompilation.MAIN_COMPILATION_NAME, configure)

    /**
     * Creates [JavaExec] task to run configured in the [KotlinJvmBinariesDsl] spec class from this target
     * compilation with name equals [compilationName].
     */
    fun executable(
        compilationName: String,
        configure: KotlinJvmBinaryDsl.() -> Unit
    ): TaskProvider<JavaExec>

    /**
     * Creates [JavaExec] task to run configured in the [KotlinJvmBinariesDsl] spec class from this target
     * compilation with name equals [compilationName].
     */
    fun executable(
        compilationName: String,
        configure: Action<KotlinJvmBinaryDsl>
    ): TaskProvider<JavaExec> = executable(compilationName) { configure.execute(this) }
}

internal fun ObjectFactory.DefaultKotlinJvmBinariesDsl(
    jvmCompilations: NamedDomainObjectCollection<KotlinJvmCompilation>
) = newInstance<DefaultKotlinJvmBinariesDsl>(jvmCompilations)

internal abstract class DefaultKotlinJvmBinariesDsl @Inject constructor(
    private val objectFactory: ObjectFactory,
    private val taskContainer: TaskContainer,
    private val jvmCompilations: NamedDomainObjectCollection<KotlinJvmCompilation>,
) : KotlinJvmBinariesDsl {
    override fun executable(
        compilationName: String,
        configure: KotlinJvmBinaryDsl.() -> Unit,
    ): TaskProvider<JavaExec> {
        val jvmCompilation = jvmCompilations.getByName(compilationName)

        val binarySpec = objectFactory.newInstance<KotlinJvmBinaryDsl>()
        configure(binarySpec)

        return registerJvmRunTask(
            jvmCompilation.runTaskName,
            jvmCompilation,
            binarySpec
        )
    }

    // null is only used in JS only projects
    private val KotlinJvmCompilation.runTaskName get() = "run${target.disambiguationClassifier!!.capitalize()}${name.capitalize()}"

    private fun registerJvmRunTask(
        taskName: String,
        jvmCompilation: KotlinJvmCompilation,
        jvmBinarySpec: KotlinJvmBinaryDsl,
    ): TaskProvider<JavaExec> = taskContainer.register(taskName, JavaExec::class.java) { runTask ->
        runTask.group = APPLICATION_GROUP
        runTask.description = "Run Kotlin ${jvmCompilation.disambiguatedName} as a JVM application."

        runTask.classpath(runTask.project.calculateRunClasspath(jvmBinarySpec, jvmCompilation))
        runTask.mainModule.set(jvmBinarySpec.mainModule)
        runTask.mainClass.set(jvmBinarySpec.mainClass)
        runTask.compatibilityWrapper().setJvmArgumentsConvention(jvmBinarySpec.applicationDefaultJvmArgs)

        val javaPluginExtension = runTask.project.extensions.getByType(JavaPluginExtension::class.java)
        runTask.modularity.inferModulePath.convention(javaPluginExtension.modularity.inferModulePath)

        runTask.configureTaskToolchain()
    }

    private fun Project.calculateRunClasspath(
        jvmBinarySpec: KotlinJvmBinaryDsl,
        jvmCompilation: KotlinJvmCompilation,
    ): FileCollection {

        val jarTask = tasks.locateTask<Jar>(jvmCompilation.jarTaskName)
        return objects.fileCollection().from(
            {
                // Quote from commit 5aa762bf4604 in Gradle:
                // By default, Java expects everything that belongs to one module to be
                //    in the same location (Jar or folder). Since in Gradle resources
                //    or outputs from different compilation steps are in separate folders,
                //    everything that is not in the same 'classes' folder as the
                //    module-info.class becomes unaccessible. Hence, we always use the complete
                //    Jar now to run a module via the :run task.
                if (jvmBinarySpec.mainModule.isPresent && jarTask != null) {
                    arrayOf(
                        jarTask.map { it.archiveFile },
                        jvmCompilation.runtimeDependencyFiles,
                    )
                } else {
                    arrayOf(
                        jvmCompilation.output.allOutputs,
                        jvmCompilation.runtimeDependencyFiles,
                    )
                }
            }
        )
    }

    private fun JavaExec.configureTaskToolchain() {
        val toolchainService = project.extensions.getByType(JavaToolchainService::class.java)
        val toolchain = project.extensions.getByType(JavaPluginExtension::class.java).toolchain
        javaLauncher.convention(toolchainService.launcherFor(toolchain))
    }
}