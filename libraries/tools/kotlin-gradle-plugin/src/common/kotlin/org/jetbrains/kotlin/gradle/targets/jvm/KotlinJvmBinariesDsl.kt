/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.jvm

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.distribution.DistributionContainer
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ApplicationPlugin.APPLICATION_GROUP
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.PluginManager
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.application.CreateStartScripts
import org.gradle.api.tasks.bundling.Jar
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinGradlePluginDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.internal.compatibilityWrapper
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.registerArchiveTask
import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.utils.getByType
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.gradle.utils.onlyJars
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
     *
     * The default value is "bin".
     */
    val executableDir: Property<String>

    /**
     * The specification of the contents of the distribution.
     *
     * Use this [CopySpec] to include extra files/resource in the application distribution:
     * ```
     * kotlin {
     *    jvm {
     *      binaries {
     *         executable {
     *            mainClass.set("foo.MainKt")
     *            applicationDistribution.from("some/dir") {
     *                it.include("*.txt")
     *            }
     *         }
     *      }
     *    }
     *  }
     * ```
     *
     * Note that the application plugin pre-configures this spec to include the contents of "src/dist",
     * copy the application start scripts into the "bin" directory,
     * and copy the built jar and its dependencies into the "lib" directory.
     */
    var applicationDistribution: CopySpec
}

@KotlinGradlePluginDsl
interface KotlinJvmBinariesDsl {
    /**
     * Creates [JavaExec] task to run configured in the [KotlinJvmBinariesDsl] spec class from this target
     * main compilation.
     */
    fun executable(
        configure: KotlinJvmBinaryDsl.() -> Unit
    ): TaskProvider<JavaExec> = executable(KotlinCompilation.MAIN_COMPILATION_NAME, configure = configure)

    /**
     * Creates [JavaExec] task to run configured in the [KotlinJvmBinariesDsl] spec class from this target
     * main compilation.
     */
    fun executable(
        configure: Action<KotlinJvmBinaryDsl>
    ): TaskProvider<JavaExec> = executable(KotlinCompilation.MAIN_COMPILATION_NAME, configure = configure)

    /**
     * Creates [JavaExec] task to run configured in the [KotlinJvmBinariesDsl] spec class from this target
     * compilation with name equals [compilationName].
     */
    fun executable(
        compilationName: String,
        configure: KotlinJvmBinaryDsl.() -> Unit
    ): TaskProvider<JavaExec> = executable(compilationName, disambiguationSuffix = "", configure = configure)

    /**
     * Creates [JavaExec] task to run configured in the [KotlinJvmBinariesDsl] spec class from this target
     * compilation with name equals [compilationName].
     */
    fun executable(
        compilationName: String,
        configure: Action<KotlinJvmBinaryDsl>
    ): TaskProvider<JavaExec> = executable(compilationName, disambiguationSuffix = "", configure = configure)

    /**
     * Creates [JavaExec] task to run configured in the [KotlinJvmBinariesDsl] spec class from this target
     * compilation with name equals [compilationName].
     *
     * @param disambiguationSuffix should be used to distinguish between different executable for the same compilation.
     * This suffix is used as a last part in executable names - for example,
     * for compilation "custom" and with disambiguation suffix, "another" [JavaExec] task name will be "runJvmCustomAnother".
     */
    fun executable(
        compilationName: String,
        disambiguationSuffix: String = "",
        configure: KotlinJvmBinaryDsl.() -> Unit
    ): TaskProvider<JavaExec>

    /**
     * Creates [JavaExec] task to run configured in the [KotlinJvmBinariesDsl] spec class from this target
     * compilation with name equals [compilationName].
     *
     * @param disambiguationSuffix should be used to distinguish between different executable for the same compilation.
     * This suffix is used as a last part in executable names - for example,
     * for compilation "custom" and with disambiguation suffix, "another" [JavaExec] task name will be "runJvmCustomAnother".
     */
    fun executable(
        compilationName: String,
        disambiguationSuffix: String = "",
        configure: Action<KotlinJvmBinaryDsl>
    ): TaskProvider<JavaExec> = executable(compilationName, disambiguationSuffix) { configure.execute(this) }
}

internal fun ObjectFactory.DefaultKotlinJvmBinariesDsl(
    jvmCompilations: NamedDomainObjectCollection<KotlinJvmCompilation>,
    project: Project,
) = newInstance<DefaultKotlinJvmBinariesDsl>(jvmCompilations, project)

internal abstract class DefaultKotlinJvmBinariesDsl @Inject constructor(
    private val objectFactory: ObjectFactory,
    private val taskContainer: TaskContainer,
    private val pluginManager: PluginManager,
    private val jvmCompilations: NamedDomainObjectCollection<KotlinJvmCompilation>,
    private val project: Project,
) : KotlinJvmBinariesDsl {

    init {
        pluginManager.apply("distribution")
    }

    private val distributionContainer = project.extensions.getByType<DistributionContainer>()

    override fun executable(
        compilationName: String,
        disambiguationSuffix: String,
        configure: KotlinJvmBinaryDsl.() -> Unit,
    ): TaskProvider<JavaExec> {
        applyDistributionPluginIfMissing()
        val jvmCompilation = jvmCompilations.getByName(compilationName)

        val binarySpec = objectFactory.newInstance<KotlinJvmBinaryDsl>()
        binarySpec.applicationName.convention(project.name)
        binarySpec.executableDir.convention("bin")
        binarySpec.applicationDistribution = project.copySpec()
        configure(binarySpec)

        val jarTask = jvmCompilation.jarTask
        registerDistribution(
            jvmCompilation.distributionName(disambiguationSuffix),
            binarySpec,
            jvmCompilation,
            jarTask,
            disambiguationSuffix,
        )

        return registerJvmRunTask(
            jvmCompilation.runTaskName(disambiguationSuffix),
            jvmCompilation,
            binarySpec,
            jarTask,
        )
    }

    // null is only used in JS only projects
    private fun KotlinJvmCompilation.runTaskName(
        disambiguationSuffix: String
    ) = "run${disambiguateName(disambiguationSuffix.capitalizeAsciiOnly()).capitalizeAsciiOnly()}"

    private fun KotlinJvmCompilation.distributionName(
        disambiguationSuffix: String
    ) = disambiguateName(disambiguationSuffix)

    private fun registerJvmRunTask(
        taskName: String,
        jvmCompilation: KotlinJvmCompilation,
        jvmBinarySpec: KotlinJvmBinaryDsl,
        compilationJarTask: TaskProvider<Jar>,
    ): TaskProvider<JavaExec> = taskContainer.register(taskName, JavaExec::class.java) { runTask ->
        runTask.group = APPLICATION_GROUP
        runTask.description = "Run Kotlin ${jvmCompilation.disambiguatedName} as a JVM application."

        runTask.classpath(runTask.project.calculateRunClasspath(jvmBinarySpec, jvmCompilation, compilationJarTask))
        runTask.mainModule.set(jvmBinarySpec.mainModule)
        runTask.mainClass.set(jvmBinarySpec.mainClass)
        runTask.compatibilityWrapper().setJvmArgumentsConvention(jvmBinarySpec.applicationDefaultJvmArgs)

        val javaPluginExtension = runTask.project.extensions.getByType(JavaPluginExtension::class.java)
        runTask.modularity.inferModulePath.convention(javaPluginExtension.modularity.inferModulePath)

        runTask.configureTaskToolchain()
    }

    private val KotlinJvmCompilation.jarTask: TaskProvider<Jar>
        get() = archiveTaskName?.let { this@DefaultKotlinJvmBinariesDsl.taskContainer.locateTask(it) }
            ?: registerArchiveTask(project)

    private fun Project.calculateRunClasspath(
        jvmBinarySpec: KotlinJvmBinaryDsl,
        jvmCompilation: KotlinJvmCompilation,
        compilationJarTask: TaskProvider<Jar>,
    ): FileCollection {

        return objects.fileCollection().from(
            {
                // Quote from commit 5aa762bf4604 in Gradle:
                // By default, Java expects everything that belongs to one module to be
                //    in the same location (Jar or folder). Since in Gradle resources
                //    or outputs from different compilation steps are in separate folders,
                //    everything that is not in the same 'classes' folder as the
                //    module-info.class becomes unaccessible. Hence, we always use the complete
                //    Jar now to run a module via the :run task.
                if (jvmBinarySpec.mainModule.isPresent) {
                    jarOnlyClasspath(
                        jvmCompilation,
                        compilationJarTask,
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

    private fun jarOnlyClasspath(
        jvmCompilation: KotlinJvmCompilation,
        compilationJarTask: TaskProvider<Jar>,
    ): FileCollection {
        val classpath = jvmCompilation.project.objects.fileCollection()
        classpath.from(compilationJarTask)
        classpath.from(jvmCompilation.runtimeDependencyFiles.onlyJars)
        jvmCompilation
            .allAssociatedCompilations
            .forAll { compilation ->
                if (compilation is KotlinJvmCompilation) {
                    classpath.from(compilation.jarTask)
                    classpath.from(jvmCompilation.runtimeDependencyFiles.onlyJars)
                }
            }
        return classpath
    }

    private fun JavaExec.configureTaskToolchain() {
        val toolchainService = project.extensions.getByType(JavaToolchainService::class.java)
        val toolchain = project.extensions.getByType(JavaPluginExtension::class.java).toolchain
        javaLauncher.convention(toolchainService.launcherFor(toolchain))
    }

    private fun registerDistribution(
        distributionName: String,
        jvmBinarySpec: KotlinJvmBinaryDsl,
        jvmCompilation: KotlinJvmCompilation,
        compilationJarTask: TaskProvider<Jar>,
        disambiguationSuffix: String,
    ) {
        val createStartScriptsTask = registerDistributionScriptsTask(
            distributionName,
            jvmBinarySpec,
            jvmCompilation,
            compilationJarTask,
        )
        distributionContainer.register(distributionName) { distribution ->
            distribution.distributionBaseName.convention(jvmBinarySpec.applicationName)
            val distSpec = distribution.contents
            distribution.distributionClassifier.convention(jvmCompilation.disambiguateName(disambiguationSuffix))

            val libChildSpec = project.copySpec().into("lib")
            libChildSpec.from(jarOnlyClasspath(jvmCompilation, compilationJarTask))

            val binChildSpec = project.copySpec()
            binChildSpec.into(jvmBinarySpec.executableDir)
            binChildSpec.from(createStartScriptsTask)
            if (GradleVersion.current() >= GradleVersion.version("8.3")) {
                binChildSpec.filePermissions { it.unix("rwxr-xr-x") }
            } else {
                @Suppress("DEPRECATION")
                binChildSpec.fileMode = 0b111_101_101 // rwxr-xr-x
            }

            val childSpec = project.copySpec()
            childSpec.from(project.file("src/dist"))
            childSpec.with(libChildSpec)
            childSpec.with(binChildSpec)

            distSpec.with(childSpec)
            distSpec.with(jvmBinarySpec.applicationDistribution)
        }
    }

    private fun registerDistributionScriptsTask(
        distributionName: String,
        jvmBinarySpec: KotlinJvmBinaryDsl,
        jvmCompilation: KotlinJvmCompilation,
        compilationJarTask: TaskProvider<Jar>,
    ): TaskProvider<CreateStartScripts> {
        return taskContainer.register("startScriptsFor${distributionName.capitalizeAsciiOnly()}", CreateStartScripts::class.java) { task ->
            task.description = "Creates OS specific scripts to run the project/${distributionName} as a JVM application."

            task.classpath = jarOnlyClasspath(jvmCompilation, compilationJarTask)
            task.mainClass.convention(jvmBinarySpec.mainClass)
            task.mainModule.convention(jvmBinarySpec.mainModule)
            task.applicationName = jvmBinarySpec.applicationName.get()
            task.outputDir = project.layout.buildDirectory.dir("${distributionName}/scripts").get().asFile
            task.executableDir = jvmBinarySpec.executableDir.get()
            task.defaultJvmOpts = jvmBinarySpec.applicationDefaultJvmArgs.get()

            val javaPluginExtension = project.extensions.getByType(JavaPluginExtension::class.java)
            task.modularity.inferModulePath.convention(javaPluginExtension.modularity.inferModulePath)
        }
    }

    private fun applyDistributionPluginIfMissing() {
        if (!pluginManager.hasPlugin("distribution"))
            pluginManager.apply("distribution")
    }
}