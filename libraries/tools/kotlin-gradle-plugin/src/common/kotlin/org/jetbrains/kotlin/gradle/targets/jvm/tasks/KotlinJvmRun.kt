package org.jetbrains.kotlin.gradle.targets.jvm.tasks

import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.KotlinJvmMainRunTaskConflict
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.gradle.plugin.internal.configurationTimePropertiesAccessor
import org.jetbrains.kotlin.gradle.plugin.internal.usedAtConfigurationTime
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.utils.getByType
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.tooling.core.UnsafeApi

/**
 * Run task for executing Kotlin/Jvm code.
 * Shall be configured using the [KotlinJvmRunDsl]
 *
 * ### IDE carrier task:
 * This task can act as 'carrier' task for IDE run gutters:
 * It accepts configurable 'mainClass' when a Gradle property is provided as
 * `<taskName>.mainClass=...`
 *
 * e.g.
 * ```
 * ./gradlew runJvm // <- executes the mainClass that is configured by default by the user
 * ./gradlew runJvm -PrunJvm.mainClass="FooKt" // <- executes FooKt.main
 * ./gradlew runJvm -PmainClass="FooKt" // <- excutes FooKt.main
 * ```
 */
@InternalKotlinGradlePluginApi
@DisableCachingByDefault
open class KotlinJvmRun : JavaExec()

@ExperimentalKotlinGradlePluginApi
interface KotlinJvmRunDsl {
    /**
     * ## See [JavaExec.mainClass]
     */
    val mainClass: Property<String>

    /**
     * ## See [JavaExec.args]
     */
    fun args(vararg args: Any)

    /**
     * ## See [JavaExec.args]
     */
    fun args(args: Iterable<*>)

    /**
     * ## See [JavaExec.setArgs]
     */
    fun setArgs(args: Iterable<*>)

    /**
     * ## See [JavaExec.classpath]
     */
    fun classpath(vararg paths: Any)


    /**
     * ## See [JavaExec.setClasspath]
     */
    fun setClasspath(classpath: FileCollection)

    /**
     * Adds the runtime classpath of the given [compilation] to this run task
     */
    fun classpath(compilation: KotlinCompilation<*>)
}

internal suspend fun KotlinJvmTarget.registerMainRunTask(): KotlinJvmRunDslImpl? {
    /* Await all inputs from user */
    KotlinPluginLifecycle.Stage.FinaliseDsl.await()
    val mainCompilation = compilations.findByName("main") ?: return null
    val taskName = lowerCamelCaseName(targetName, "run")

    if (taskName in project.tasks.names) {
        /* Proper warning tbd */
        project.reportDiagnostic(KotlinJvmMainRunTaskConflict(targetName = name, taskName = taskName))
        return null
    }

    return registerKotlinJvmRun(taskName, mainCompilation)
}

private fun KotlinJvmTarget.registerKotlinJvmRun(taskName: String, compilation: KotlinJvmCompilation): KotlinJvmRunDslImpl {
    val mainClass = project.objects.property<String>()
    val taskProvider = project.tasks.register(taskName, KotlinJvmRun::class.java)

    /* Convenience helper for telling older Gradle versions, that this provider is used at configuration time */
    val configurationTimePropertiesAccessor = project.configurationTimePropertiesAccessor
    fun <T> Provider<T>.usedAtConfigurationTime() = usedAtConfigurationTime(configurationTimePropertiesAccessor)

    taskProvider.configure { task ->
        task.group = "run"
        task.description = "Jvm Run task for target '${compilation.target.name}' and compilation '${compilation.name}'. " +
                "This task can act as carrier for the IDE to execute jvm based code"

        /**
         * See [KotlinJvmRun]: This task will respect the '<taskName>.mainClass' property over
         * the DSL configuration
         */
        task.mainClass.value(
            project.providers.gradleProperty("$taskName.mainClass").usedAtConfigurationTime()
                .orElse(project.providers.gradleProperty("mainClass").usedAtConfigurationTime())
                .orElse(project.providers.systemProperty("$taskName.mainClass").usedAtConfigurationTime())
                .orElse(project.providers.systemProperty("mainClass").usedAtConfigurationTime())
                .orElse(mainClass).usedAtConfigurationTime()
        )

        project.extensions.findByType(JavaToolchainService::class.java)?.let { toolchainService ->
            val toolchain = project.extensions.getByType(JavaPluginExtension::class.java).toolchain
            task.javaLauncher.convention(toolchainService.launcherFor(toolchain))
        }
    }

    @OptIn(UnsafeApi::class)
    return KotlinJvmRunDslImpl(taskProvider, mainClass)
        .also { dsl -> dsl.classpath(compilation) }
}

/**
 * Use [registerKotlinJvmRun] to create an instance
 */
internal class KotlinJvmRunDslImpl @UnsafeApi constructor(
    val task: TaskProvider<KotlinJvmRun>,
    override val mainClass: Property<String>,
) : KotlinJvmRunDsl {

    override fun args(vararg args: Any) {
        task.configure { it.args(*args) }
    }

    override fun args(args: Iterable<*>) {
        task.configure { it.args(args) }
    }

    override fun setArgs(args: Iterable<*>) {
        task.configure { it.setArgs(args) }
    }

    override fun classpath(vararg paths: Any) {
        task.configure { it.classpath(*paths) }
    }

    override fun setClasspath(classpath: FileCollection) {
        task.configure { it.setClasspath(classpath) }
    }

    override fun classpath(compilation: KotlinCompilation<*>) {
        task.configure {
            it.classpath(compilation.output.allOutputs)
            it.classpath(compilation.internal.configurations.runtimeDependencyConfiguration)
        }
    }
}
