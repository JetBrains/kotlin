/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.internal.plugins.DslObject
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainSpec
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsSingleTargetPreset
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinPm20ProjectExtension
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.targets.js.calculateJsCompilerType
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrSingleTargetPreset
import org.jetbrains.kotlin.gradle.tasks.CompileUsingKotlinDaemon
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.castIsolatedKotlinPluginClassLoaderAware
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import javax.inject.Inject
import kotlin.reflect.KClass

private const val KOTLIN_PROJECT_EXTENSION_NAME = "kotlin"

internal fun Project.createKotlinExtension(extensionClass: KClass<out KotlinTopLevelExtension>): KotlinTopLevelExtension {
    return extensions.create(KOTLIN_PROJECT_EXTENSION_NAME, extensionClass.java, this)
}

internal val Project.topLevelExtension: KotlinTopLevelExtension
    get() = extensions.getByName(KOTLIN_PROJECT_EXTENSION_NAME).castIsolatedKotlinPluginClassLoaderAware()

internal val Project.topLevelExtensionOrNull: KotlinTopLevelExtension?
    get() = extensions.findByName(KOTLIN_PROJECT_EXTENSION_NAME)?.castIsolatedKotlinPluginClassLoaderAware<KotlinTopLevelExtension>()

internal val Project.kotlinExtensionOrNull: KotlinProjectExtension?
    get() = extensions.findByName(KOTLIN_PROJECT_EXTENSION_NAME)?.castIsolatedKotlinPluginClassLoaderAware()

val Project.kotlinExtension: KotlinProjectExtension
    get() = extensions.getByName(KOTLIN_PROJECT_EXTENSION_NAME).castIsolatedKotlinPluginClassLoaderAware()

internal val Project.kotlinJvmExtensionOrNull: KotlinJvmProjectExtension?
    get() = extensions.findByName(KOTLIN_PROJECT_EXTENSION_NAME)?.castIsolatedKotlinPluginClassLoaderAware()

internal val Project.kotlinJvmExtension: KotlinJvmProjectExtension
    get() = extensions.getByName(KOTLIN_PROJECT_EXTENSION_NAME).castIsolatedKotlinPluginClassLoaderAware()

internal val Project.multiplatformExtensionOrNull: KotlinMultiplatformExtension?
    get() = extensions.findByName(KOTLIN_PROJECT_EXTENSION_NAME)?.castIsolatedKotlinPluginClassLoaderAware()

internal val Project.multiplatformExtension: KotlinMultiplatformExtension
    get() = extensions.getByName(KOTLIN_PROJECT_EXTENSION_NAME).castIsolatedKotlinPluginClassLoaderAware()

internal val Project.pm20Extension: KotlinPm20ProjectExtension
    get() = extensions.getByName(KOTLIN_PROJECT_EXTENSION_NAME).castIsolatedKotlinPluginClassLoaderAware()

internal val Project.pm20ExtensionOrNull: KotlinPm20ProjectExtension?
    get() = extensions.findByName(KOTLIN_PROJECT_EXTENSION_NAME)?.castIsolatedKotlinPluginClassLoaderAware()

abstract class KotlinTopLevelExtension(internal val project: Project) : KotlinTopLevelExtensionConfig {

    override lateinit var coreLibrariesVersion: String

    private val toolchainSupport = ToolchainSupport.createToolchain(project)

    /**
     * Configures [Java toolchain](https://docs.gradle.org/current/userguide/toolchains.html) both for Kotlin JVM and Java tasks.
     *
     * @param action - action to configure [JavaToolchainSpec]
     */
    fun jvmToolchain(action: Action<JavaToolchainSpec>) {
        toolchainSupport.applyToolchain(action)
    }

    /**
     * Configures [Java toolchain](https://docs.gradle.org/current/userguide/toolchains.html) both for Kotlin JVM and Java tasks.
     *
     * @param jdkVersion - jdk version as number. For example, 17 for Java 17.
     */
    fun jvmToolchain(jdkVersion: Int) {
        jvmToolchain {
            it.languageVersion.set(JavaLanguageVersion.of(jdkVersion))
        }
    }

    /**
     * Configures Kotlin daemon JVM arguments for all tasks in this project.
     *
     * **Note**: In case other projects are using different JVM arguments, new instance of Kotlin daemon will be started.
     */
    @get:JvmSynthetic
    var kotlinDaemonJvmArgs: List<String>
        @Deprecated("", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        set(value) {
            project
                .tasks
                .withType<CompileUsingKotlinDaemon>()
                .configureEach {
                    it.kotlinDaemonJvmArguments.set(value)
                }
        }

    override var explicitApi: ExplicitApiMode? = null

    override fun explicitApi() {
        explicitApi = ExplicitApiMode.Strict
    }

    override fun explicitApiWarning() {
        explicitApi = ExplicitApiMode.Warning
    }

    /**
     * Can be used to configure objects that are not yet created, or will be created in
     * 'afterEvaluate' (e.g. typically Android source sets containing flavors and buildTypes)
     *
     * Will fail project evaluation if the domain object is not created before 'afterEvaluate' listeners in the buildscript.
     *
     * @param configure: Called inline, if the value is already present. Called once the domain object is created.
     */
    @ExperimentalKotlinGradlePluginApi
    fun <T : Named> NamedDomainObjectContainer<T>.invokeWhenCreated(name: String, configure: T.() -> Unit) {
        configureEach { if (it.name == name) it.configure() }
        project.launchInStage(KotlinPluginLifecycle.Stage.ReadyForExecution) {
            if (name !in names) {
                /* Expect 'named' to throw corresponding exception */
                named(name).configure(configure)
            }
        }
    }
}

open class KotlinProjectExtension @Inject constructor(project: Project) : KotlinTopLevelExtension(project), KotlinSourceSetContainer {
    override var sourceSets: NamedDomainObjectContainer<KotlinSourceSet>
        @Suppress("UNCHECKED_CAST")
        get() = DslObject(this).extensions.getByName("sourceSets") as NamedDomainObjectContainer<KotlinSourceSet>
        internal set(value) {
            DslObject(this).extensions.add("sourceSets", value)
        }

    internal suspend fun awaitSourceSets(): NamedDomainObjectContainer<KotlinSourceSet> {
        KotlinPluginLifecycle.Stage.AfterFinaliseDsl.await()
        return sourceSets
    }
}

abstract class KotlinSingleTargetExtension<TARGET : KotlinTarget>(project: Project) : KotlinProjectExtension(project) {
    abstract val target: TARGET

    fun target(body: Action<TARGET>) = body.execute(target)
}

abstract class KotlinSingleJavaTargetExtension(project: Project) : KotlinSingleTargetExtension<KotlinWithJavaTarget<*, *>>(project)

abstract class KotlinJvmProjectExtension(project: Project) : KotlinSingleJavaTargetExtension(project) {
    override lateinit var target: KotlinWithJavaTarget<KotlinJvmOptions, KotlinJvmCompilerOptions>
        internal set

    open fun target(body: KotlinWithJavaTarget<KotlinJvmOptions, KotlinJvmCompilerOptions>.() -> Unit) = target.run(body)

    val compilerOptions: KotlinJvmCompilerOptions =
        project.objects.newInstance(KotlinJvmCompilerOptionsDefault::class.java)

    fun compilerOptions(configure: Action<KotlinJvmCompilerOptions>) {
        configure.execute(compilerOptions)
    }

    fun compilerOptions(configure: KotlinJvmCompilerOptions.() -> Unit) {
        configure(compilerOptions)
    }
}

abstract class Kotlin2JsProjectExtension(project: Project) : KotlinSingleJavaTargetExtension(project) {
    private lateinit var _target: KotlinWithJavaTarget<KotlinJsOptions, KotlinJsCompilerOptions>

    override val target: KotlinWithJavaTarget<KotlinJsOptions, KotlinJsCompilerOptions>
        get() {
            if (!::_target.isInitialized) throw IllegalStateException("Extension target is not initialized!")

            return _target
        }

    internal fun setTarget(target: KotlinWithJavaTarget<KotlinJsOptions, KotlinJsCompilerOptions>) {
        _target = target
    }

    open fun target(body: KotlinWithJavaTarget<KotlinJsOptions, KotlinJsCompilerOptions>.() -> Unit) = target.run(body)
}

abstract class KotlinJsProjectExtension(project: Project) :
    KotlinSingleTargetExtension<KotlinJsTargetDsl>(project),
    KotlinJsCompilerTypeHolder {
    lateinit var irPreset: KotlinJsIrSingleTargetPreset

    lateinit var legacyPreset: KotlinJsSingleTargetPreset

    private val targetSetObservers = mutableListOf<(KotlinJsTargetDsl?) -> Unit>()

    // target is public property
    // Users can write kotlin.target and it should work
    // So call of target should init default configuration
    @Deprecated("Use `target` instead", ReplaceWith("target"))
    var _target: KotlinJsTargetDsl? = null
        private set(value) {
            field = value
            targetSetObservers.forEach { it(value) }
        }

    fun registerTargetObserver(observer: (KotlinJsTargetDsl?) -> Unit) {
        targetSetObservers.add(observer)
    }

    companion object {
        internal fun reportJsCompilerMode(compilerType: KotlinJsCompilerType) {
            when (compilerType) {
                KotlinJsCompilerType.LEGACY -> KotlinBuildStatsService.getInstance()?.report(StringMetrics.JS_COMPILER_MODE, "legacy")
                KotlinJsCompilerType.IR -> KotlinBuildStatsService.getInstance()?.report(StringMetrics.JS_COMPILER_MODE, "ir")
                KotlinJsCompilerType.BOTH -> KotlinBuildStatsService.getInstance()?.report(StringMetrics.JS_COMPILER_MODE, "both")
            }
        }

        internal fun warnAboutDeprecatedCompiler(project: Project, compilerType: KotlinJsCompilerType) {
            if (PropertiesProvider(project).jsCompilerNoWarn) return
            val logger = project.logger
            when (compilerType) {
                KotlinJsCompilerType.LEGACY -> logger.warn(LEGACY_DEPRECATED)
                KotlinJsCompilerType.IR -> {}
                KotlinJsCompilerType.BOTH -> logger.warn(BOTH_DEPRECATED)
            }
        }

        private val LEGACY_DEPRECATED =
            """
                |
                |==========
                |This project currently uses the Kotlin/JS Legacy compiler backend, which has been deprecated and will be removed in a future release.
                |
                |Please migrate the project to the new IR-based compiler (https://kotl.in/jsir).
                |==========
                |
            """.trimMargin()

        private val BOTH_DEPRECATED =
            """
                |
                |==========
                |This project currently uses Both mode, which requires the Kotlin/JS Legacy compiler backend.
                |This backend has been deprecated and will be removed in a future release.
                |
                |Please migrate the project to the new IR-based compiler (https://kotl.in/jsir).
                |==========
                |
            """.trimMargin()
    }

    @Deprecated("Use js() instead", ReplaceWith("js()"))
    @Suppress("DEPRECATION")
    override val target: KotlinJsTargetDsl
        get() {
            if (_target == null) {
                js {}
            }
            return _target!!
        }

    override val compilerTypeFromProperties: KotlinJsCompilerType? = project.kotlinPropertiesProvider.jsCompiler

    @Suppress("DEPRECATION")
    private fun jsInternal(
        compiler: KotlinJsCompilerType? = null,
        body: KotlinJsTargetDsl.() -> Unit
    ): KotlinJsTargetDsl {
        if (_target != null) {
            val previousCompilerType = _target!!.calculateJsCompilerType()
            check(compiler == null || previousCompilerType == compiler) {
                "You already registered Kotlin/JS target with another compiler: ${previousCompilerType.lowerName}"
            }
        }

        if (_target == null) {
            val compilerOrFromProperties = compiler ?: compilerTypeFromProperties
            val compilerOrDefault = compilerOrFromProperties ?: defaultJsCompilerType
            reportJsCompilerMode(compilerOrDefault)
            warnAboutDeprecatedCompiler(project, compilerOrDefault)
            val target: KotlinJsTargetDsl = when (compilerOrDefault) {
                KotlinJsCompilerType.LEGACY -> legacyPreset
                    .also {
                        it.irPreset = null
                    }
                    .createTarget("js")

                KotlinJsCompilerType.IR -> irPreset
                    .also {
                        it.mixedMode = false
                    }
                    .createTarget("js")

                KotlinJsCompilerType.BOTH -> legacyPreset
                    .also {
                        irPreset.mixedMode = true
                        it.irPreset = irPreset
                    }
                    .createTarget(
                        lowerCamelCaseName(
                            "js",
                            LEGACY.lowerName
                        )
                    )
            }

            this._target = target

            target.project.components.addAll(target.components)
        }

        target.run(body)

        return target
    }

    fun js(
        compiler: KotlinJsCompilerType = defaultJsCompilerType,
        body: KotlinJsTargetDsl.() -> Unit = { }
    ): KotlinJsTargetDsl = jsInternal(compiler, body)

    fun js(
        compiler: String,
        body: KotlinJsTargetDsl.() -> Unit = { }
    ): KotlinJsTargetDsl = js(
        KotlinJsCompilerType.byArgument(compiler),
        body
    )

    fun js(
        body: KotlinJsTargetDsl.() -> Unit = { }
    ) = jsInternal(body = body)

    fun js() = js { }

    fun js(compiler: KotlinJsCompilerType, configure: Action<KotlinJsTargetDsl>) =
        js(compiler = compiler) {
            configure.execute(this)
        }

    fun js(compiler: String, configure: Action<KotlinJsTargetDsl>) =
        js(compiler = compiler) {
            configure.execute(this)
        }

    fun js(configure: Action<KotlinJsTargetDsl>) = jsInternal {
        configure.execute(this)
    }

    @Deprecated("Use js instead", ReplaceWith("js(body)"))
    open fun target(body: KotlinJsTargetDsl.() -> Unit) = js(body)

    @Deprecated(
        "Needed for IDE import using the MPP import mechanism",
        level = DeprecationLevel.HIDDEN
    )
    @Suppress("DEPRECATION")
    fun getTargets(): NamedDomainObjectContainer<KotlinTarget>? =
        _target?.let { target ->
            target.project.container(KotlinTarget::class.java)
                .apply { add(target) }
        }
}

abstract class KotlinCommonProjectExtension(project: Project) : KotlinSingleJavaTargetExtension(project) {
    override lateinit var target: KotlinWithJavaTarget<KotlinMultiplatformCommonOptions, KotlinMultiplatformCommonCompilerOptions>
        internal set

    open fun target(
        body: KotlinWithJavaTarget<KotlinMultiplatformCommonOptions, KotlinMultiplatformCommonCompilerOptions>.() -> Unit
    ) = target.run(body)
}

abstract class KotlinAndroidProjectExtension(project: Project) : KotlinSingleTargetExtension<KotlinAndroidTarget>(project) {
    override lateinit var target: KotlinAndroidTarget
        internal set

    open fun target(body: KotlinAndroidTarget.() -> Unit) = target.run(body)

    val compilerOptions: KotlinJvmCompilerOptions =
        project.objects.newInstance(KotlinJvmCompilerOptionsDefault::class.java)

    fun compilerOptions(configure: Action<KotlinJvmCompilerOptions>) {
        configure.execute(compilerOptions)
    }

    fun compilerOptions(configure: KotlinJvmCompilerOptions.() -> Unit) {
        configure(compilerOptions)
    }
}

enum class NativeCacheKind(val produce: String?, val outputKind: CompilerOutputKind?) {
    NONE(null, null),
    DYNAMIC("dynamic_cache", CompilerOutputKind.DYNAMIC_CACHE),
    STATIC("static_cache", CompilerOutputKind.STATIC_CACHE);

    companion object {
        fun byCompilerArgument(argument: String): NativeCacheKind? =
            NativeCacheKind.values().firstOrNull { it.name.equals(argument, ignoreCase = true) }
    }
}

// This is a temporary parameter for the translation period.
enum class NativeCacheOrchestration {
    Gradle,
    Compiler;

    companion object {
        fun byCompilerArgument(argument: String): NativeCacheOrchestration? =
            NativeCacheOrchestration.values().firstOrNull { it.name.equals(argument, ignoreCase = true) }
    }
}
