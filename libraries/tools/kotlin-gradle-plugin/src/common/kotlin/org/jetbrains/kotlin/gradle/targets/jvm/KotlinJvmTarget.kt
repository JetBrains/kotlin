/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.jvm

import org.gradle.api.Action
import org.gradle.api.InvalidUserCodeException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptionsDefault
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.*
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTarget
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmRunDsl
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmRunDslImpl
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.registerMainRunTask
import org.jetbrains.kotlin.gradle.tasks.DefaultKotlinJavaToolchain
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.gradle.utils.Future
import org.jetbrains.kotlin.gradle.utils.findAppliedAndroidPluginIdOrNull
import org.jetbrains.kotlin.gradle.utils.future
import java.util.concurrent.Callable
import javax.inject.Inject

private const val WITH_JAVA_DEPRECATION_MESSAGE =
    "Kotlin Multiplatform JVM target compiles Java sources by default. Please remove `withJava()` call."

abstract class KotlinJvmTarget @Inject constructor(
    project: Project,
) : KotlinOnlyTarget<KotlinJvmCompilation>(project, KotlinPlatformType.jvm),
    HasConfigurableKotlinCompilerOptions<KotlinJvmCompilerOptions>,
    KotlinTargetWithTests<JvmClasspathTestRunSource, KotlinJvmTestRun> {

    override val testRuns: NamedDomainObjectContainer<KotlinJvmTestRun> by lazy {
        project.container(KotlinJvmTestRun::class.java, KotlinJvmTestRunFactory(this))
    }

    internal val mainRun: Future<KotlinJvmRunDslImpl?> = project.future { registerMainRunTask() }

    /**
     * ### ⚠️ KotlinJvmTarget 'mainRun' is experimental
     * The [KotlinJvmTarget], by default, creates a 'run' task called {targetName}Run, which will allows simple
     * execution of the targets 'main' code.
     *
     * e.g.
     * ```kotlin
     * // build.gradle.kts
     * kotlin {
     *     jvm().mainRun {
     *         mainClass.set("FooKt")
     *     }
     * }
     *
     * // src/jvmMain/Foo
     * fun main() {
     *     println("Hello from foo")
     * }
     * ```
     *
     * will be executable using
     * ```text
     * ./gradlew jvmRun
     * > "Hello from foo"
     * ```
     *
     * ### Running a different 'mainClass' from CLI:
     * The execution of the main code allows providing a different 'mainClass' via CLI. *
     * It accepts System Properties and Gradle Properties. However, when Gradle Configuration Cache is used,
     * System Properties are the preferred way.
     *
     * ```text
     * ./gradlew jvmRun -DmainClass="BarKt"
     *                    ^
     *                    Will execute the 'src/jvmMain/kotlin/Bar' main method.
     * ```
     */
    @ExperimentalKotlinGradlePluginApi
    fun mainRun(configure: KotlinJvmRunDsl.() -> Unit) = project.launch {
        mainRun.await()?.configure()
    }

    private val binariesDsl by lazy {
        // lazy is required as compilation is lateinit property
        project.objects.DefaultKotlinJvmBinariesDsl(
            compilations,
            project,
        )
    }

    /**
     * Configures executable binaries and relevant tasks for this target.
     */
    @ExperimentalKotlinGradlePluginApi
    fun binaries(configure: KotlinJvmBinariesDsl.() -> Unit) {
        configure(binariesDsl)
    }

    /**
     * Configures executable binaries and relevant tasks for this target.
     */
    @ExperimentalKotlinGradlePluginApi
    fun binaries(configure: Action<KotlinJvmBinariesDsl>) {
        configure.execute(binariesDsl)
    }

    @Deprecated(
        message = WITH_JAVA_DEPRECATION_MESSAGE,
        level = DeprecationLevel.WARNING
    )
    var withJavaEnabled = false
        private set

    @Deprecated(
        message = WITH_JAVA_DEPRECATION_MESSAGE,
        level = DeprecationLevel.WARNING
    )
    fun withJava() {
        project.reportDiagnostic(KotlinToolingDiagnostics.KMPWithJavaDiagnostic())

        @Suppress("DEPRECATION")
        if (withJavaEnabled)
            return

        project.multiplatformExtension.targets.find {
            @Suppress("DEPRECATION")
            it is KotlinJvmTarget && it.withJavaEnabled
        }
            ?.let { existingJavaTarget ->
                throw InvalidUserCodeException(
                    "Only one of the JVM targets can be configured to work with Java. The target '${existingJavaTarget.name}' is " +
                            "already set up to work with Java; cannot setup another target '$targetName'"
                )
            }


        /**
         * Reports diagnostic in the case of
         * ```kotlin
         * kotlin {
         *     jvm().withJava()
         * }
         * ```
         *
         * is used together with the Android Gradle Plugin.
         * This case is incompatible so far, as the 'withJava' implementation is still using 'global' namespaces
         * (like main/test, etc), which will clash with the global names used by AGP (also occupying main, test, etc).
         */
        val trace = Throwable()
        project.launchInStage(AfterFinaliseDsl) check@{
            val androidPluginId = project.findAppliedAndroidPluginIdOrNull() ?: return@check
            project.reportDiagnostic(KotlinToolingDiagnostics.JvmWithJavaIsIncompatibleWithAndroid(androidPluginId, trace))
        }

        @Suppress("DEPRECATION")
        withJavaEnabled = true

        project.plugins.apply(JavaBasePlugin::class.java)
        val javaSourceSets = project.javaSourceSets
        AbstractKotlinPlugin.setUpJavaSourceSets(this, duplicateJavaSourceSetsAsKotlinSourceSets = false)

        // Below, some effort is made to ensure that a user or 3rd-party plugin that inspects or interacts
        // with the entities created by the Java plugin, not knowing of the existence of the Kotlin plugin,
        // sees 'the right picture' of the inputs and outputs, for example:
        // * the relevant dependencies for Java and Kotlin are in sync,
        // * the Java outputs contain the outputs produced by Kotlin as well

        javaSourceSets.all { javaSourceSet ->
            val compilation = compilations.findByName(javaSourceSet.name)
            // AbstractKotlinPlugin.setUpJavaSourceSets should already create a Kotlin compilation with 'javaSourceSet.name'.
            // If compilation is 'null' here - it means 'javaSourceSet' is from 'KotlinJvmCompilation.defaultJavaSourceSet'
            //  where the related compilation name is different. And all required configuration for this 'javaSourceSet' was already done.
            if (compilation == null) return@all
            val compileJavaTask = project.tasks.withType<AbstractCompile>().named(javaSourceSet.compileJavaTaskName)

            setupJavaSourceSetSourcesAndResources(javaSourceSet, compilation)

            val javaClasses = project.files(compileJavaTask.map { it.destinationDirectory })

            compilation.output.classesDirs.from(javaClasses)

            (javaSourceSet.output.classesDirs as? ConfigurableFileCollection)?.from(
                compilation.output.classesDirs.minus(javaClasses)
            )

            javaSourceSet.output.setResourcesDir(Callable {
                @Suppress("DEPRECATION_ERROR")
                compilation.output.resourcesDirProvider
            })

            setupDependenciesCrossInclusionForJava(compilation, javaSourceSet)
        }

        project.launchInStage(AfterFinaliseDsl) {
            javaSourceSets.all { javaSourceSet ->
                project.copyUserDefinedAttributesToJavaConfigurations(javaSourceSet, this@KotlinJvmTarget)
            }
        }

        project.plugins.withType(JavaPlugin::class.java) {
            // Eliminate the Java output configurations from dependency resolution to avoid ambiguity between them and
            // the equivalent configurations created for the target:
            project.configurations.findByName(JavaPlugin.API_ELEMENTS_CONFIGURATION_NAME)?.isCanBeConsumed = false
            project.configurations.findByName(JavaPlugin.RUNTIME_ELEMENTS_CONFIGURATION_NAME)?.isCanBeConsumed = false
            disableJavaPluginTasks(javaSourceSets)
        }

        compilations.all { compilation ->
            @Suppress("DEPRECATION")
            compilation.maybeCreateJavaSourceSet()

            /* Disabling new default Java source sets compilation tasks as they conflict with tasks created via this method */
            compilation.defaultCompileJavaProvider.configure { it.enabled = false }
        }
    }

    private fun disableJavaPluginTasks(javaSourceSet: SourceSetContainer) {
        // A 'normal' build should not do redundant job like running the tests twice or building two JARs,
        // so disable some tasks and just make them depend on the others:
        val targetJar = project.tasks.withType(Jar::class.java).named(artifactsTaskName)

        project.tasks.withType(Jar::class.java).named(javaSourceSet.getByName("main").jarTaskName) { javaJar ->
            (javaJar.source as? ConfigurableFileCollection)?.setFrom(targetJar.map { it.source })
            javaJar.archiveFileName.set(targetJar.flatMap { it.archiveFileName })
            javaJar.dependsOn(targetJar)
            javaJar.enabled = false
        }

        project.tasks.withType(Test::class.java).named(JavaPlugin.TEST_TASK_NAME) { javaTestTask ->
            javaTestTask.dependsOn(project.tasks.named(testTaskName))
            javaTestTask.enabled = false
        }
    }

    override val compilerOptions: KotlinJvmCompilerOptions = project.objects
        .newInstance<KotlinJvmCompilerOptionsDefault>()
        .apply {
            DefaultKotlinJavaToolchain.wireJvmTargetToToolchain(
                this,
                project
            )
        }
}
