/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.internal.plugins.DslObject
import org.gradle.jvm.toolchain.JavaToolchainSpec
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.plugin.*
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
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import kotlin.reflect.KClass

private const val KOTLIN_PROJECT_EXTENSION_NAME = "kotlin"

internal fun Project.createKotlinExtension(extensionClass: KClass<out KotlinTopLevelExtension>): KotlinTopLevelExtension {
    val kotlinExt = extensions.create(KOTLIN_PROJECT_EXTENSION_NAME, extensionClass.java, this)
    DslObject(kotlinExt).extensions.create("experimental", ExperimentalExtension::class.java)
    return topLevelExtension
}

internal val Project.topLevelExtension: KotlinTopLevelExtension
    get() = extensions.getByName(KOTLIN_PROJECT_EXTENSION_NAME) as KotlinTopLevelExtension

internal val Project.topLevelExtensionOrNull: KotlinTopLevelExtension?
    get() = extensions.findByName(KOTLIN_PROJECT_EXTENSION_NAME) as KotlinTopLevelExtension?

internal val Project.kotlinExtensionOrNull: KotlinProjectExtension?
    get() = extensions.findByName(KOTLIN_PROJECT_EXTENSION_NAME) as? KotlinProjectExtension

val Project.kotlinExtension: KotlinProjectExtension
    get() = extensions.getByName(KOTLIN_PROJECT_EXTENSION_NAME) as KotlinProjectExtension

internal val Project.multiplatformExtensionOrNull: KotlinMultiplatformExtension?
    get() = extensions.findByName(KOTLIN_PROJECT_EXTENSION_NAME) as? KotlinMultiplatformExtension

internal val Project.multiplatformExtension: KotlinMultiplatformExtension
    get() = extensions.getByName(KOTLIN_PROJECT_EXTENSION_NAME) as KotlinMultiplatformExtension

internal val Project.pm20Extension: KotlinPm20ProjectExtension
    get() = extensions.getByName(KOTLIN_PROJECT_EXTENSION_NAME) as KotlinPm20ProjectExtension

open class KotlinTopLevelExtension (internal val project: Project) {
    val experimental: ExperimentalExtension
        get() = DslObject(this).extensions.getByType(ExperimentalExtension::class.java)

    lateinit var coreLibrariesVersion: String

    private val toolchainSupport = ToolchainSupport.createToolchain(project)

    /**
     * Configures [Java toolchain](https://docs.gradle.org/current/userguide/toolchains.html) both for Kotlin JVM and Java tasks.
     *
     * @param action - action to configure [JavaToolchainSpec]. You could safely cast `Any` into `JavaToolchainSpec`.
     */
    fun jvmToolchain(action: Action<Any>) {
        toolchainSupport.applyToolchain(action)
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

    var explicitApi: ExplicitApiMode? = null

    fun explicitApi() {
        explicitApi = ExplicitApiMode.Strict
    }

    fun explicitApiWarning() {
        explicitApi = ExplicitApiMode.Warning
    }
}

open class KotlinProjectExtension(project: Project) : KotlinTopLevelExtension(project), KotlinSourceSetContainer {
    override var sourceSets: NamedDomainObjectContainer<KotlinSourceSet>
        @Suppress("UNCHECKED_CAST")
        get() = DslObject(this).extensions.getByName("sourceSets") as NamedDomainObjectContainer<KotlinSourceSet>
        internal set(value) {
            DslObject(this).extensions.add("sourceSets", value)
        }
}

abstract class KotlinSingleTargetExtension(project: Project) : KotlinProjectExtension(project) {
    abstract val target: KotlinTarget

    open fun target(body: Closure<out KotlinTarget>) = ConfigureUtil.configure(body, target)
}

abstract class KotlinSingleJavaTargetExtension(project: Project) : KotlinSingleTargetExtension(project) {
    abstract override val target: KotlinWithJavaTarget<*>
}

open class KotlinJvmProjectExtension(project: Project) : KotlinSingleJavaTargetExtension(project) {
    override lateinit var target: KotlinWithJavaTarget<KotlinJvmOptions>
        internal set

    open fun target(body: KotlinWithJavaTarget<KotlinJvmOptions>.() -> Unit) = target.run(body)
}

open class Kotlin2JsProjectExtension(project: Project) : KotlinSingleJavaTargetExtension(project) {
    override lateinit var target: KotlinWithJavaTarget<KotlinJsOptions>
        internal set

    open fun target(body: KotlinWithJavaTarget<KotlinJsOptions>.() -> Unit) = target.run(body)
}

open class KotlinJsProjectExtension(project: Project) :
    KotlinSingleTargetExtension(project),
    KotlinJsCompilerTypeHolder {
    lateinit var irPreset: KotlinJsIrSingleTargetPreset

    lateinit var legacyPreset: KotlinJsSingleTargetPreset

    // target is public property
    // Users can write kotlin.target and it should work
    // So call of target should init default configuration
    @Deprecated("Use `target` instead", ReplaceWith("target"))
    var _target: KotlinJsTargetDsl? = null
        private set

    companion object {
        internal fun reportJsCompilerMode(compilerType: KotlinJsCompilerType) {
            when (compilerType) {
                KotlinJsCompilerType.LEGACY -> KotlinBuildStatsService.getInstance()?.report(StringMetrics.JS_COMPILER_MODE, "legacy")
                KotlinJsCompilerType.IR -> KotlinBuildStatsService.getInstance()?.report(StringMetrics.JS_COMPILER_MODE, "ir")
                KotlinJsCompilerType.BOTH -> KotlinBuildStatsService.getInstance()?.report(StringMetrics.JS_COMPILER_MODE, "both")
            }
        }
    }

    @Deprecated("Use js() instead", ReplaceWith("js()"))
    @Suppress("DEPRECATION")
    override var target: KotlinJsTargetDsl
        get() {
            if (_target == null) {
                js {}
            }
            return _target!!
        }
        set(value) {
            _target = value
        }

    override lateinit var defaultJsCompilerType: KotlinJsCompilerType

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
            val compilerOrDefault = compiler ?: defaultJsCompilerType
            reportJsCompilerMode(compilerOrDefault)
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

    fun js(compiler: KotlinJsCompilerType, configure: Closure<*>) =
        js(compiler = compiler) {
            ConfigureUtil.configure(configure, this)
        }

    fun js(compiler: String, configure: Closure<*>) =
        js(compiler = compiler) {
            ConfigureUtil.configure(configure, this)
        }

    fun js(configure: Closure<*>) = jsInternal {
        ConfigureUtil.configure(configure, this)
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

open class KotlinCommonProjectExtension(project: Project) : KotlinSingleJavaTargetExtension(project) {
    override lateinit var target: KotlinWithJavaTarget<KotlinMultiplatformCommonOptions>
        internal set

    open fun target(body: KotlinWithJavaTarget<KotlinMultiplatformCommonOptions>.() -> Unit) = target.run(body)
}

open class KotlinAndroidProjectExtension(project: Project) : KotlinSingleTargetExtension(project) {
    override lateinit var target: KotlinAndroidTarget
        internal set

    open fun target(body: KotlinAndroidTarget.() -> Unit) = target.run(body)
}

open class ExperimentalExtension {
    var coroutines: Coroutines? = null
}

enum class Coroutines {
    ENABLE,
    WARN,
    ERROR,
    DEFAULT;

    companion object {
        fun byCompilerArgument(argument: String): Coroutines? =
            Coroutines.values().firstOrNull { it.name.equals(argument, ignoreCase = true) }
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

enum class ExplicitApiMode(private val cliOption: String) {
    Strict("strict"),
    Warning("warning"),
    Disabled("disable");

    fun toCompilerArg() = "-Xexplicit-api=$cliOption"
}
