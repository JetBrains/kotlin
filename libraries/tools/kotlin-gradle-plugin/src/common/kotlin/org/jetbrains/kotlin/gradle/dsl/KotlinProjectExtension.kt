/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import com.gradle.scan.agent.serialization.scan.serializer.kryo.it
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainSpec
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.CoroutineStart.Undispatched
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaTarget
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSetFactory
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrSingleTargetPreset
import org.jetbrains.kotlin.gradle.tasks.CompileUsingKotlinDaemon
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.gradle.utils.CompletableFuture
import org.jetbrains.kotlin.gradle.utils.Future
import org.jetbrains.kotlin.gradle.utils.castIsolatedKotlinPluginClassLoaderAware
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.tooling.core.HasMutableExtras
import org.jetbrains.kotlin.tooling.core.MutableExtras
import org.jetbrains.kotlin.tooling.core.mutableExtrasOf
import javax.inject.Inject
import kotlin.reflect.KClass

private const val KOTLIN_PROJECT_EXTENSION_NAME = "kotlin"

internal fun Project.createKotlinExtension(extensionClass: KClass<out KotlinBaseExtension>): KotlinBaseExtension {
    return extensions.create(KOTLIN_PROJECT_EXTENSION_NAME, extensionClass.java, this)
}

internal val Project.topLevelExtension: KotlinBaseExtension
    get() = extensions.getByName(KOTLIN_PROJECT_EXTENSION_NAME).castIsolatedKotlinPluginClassLoaderAware()

internal val Project.topLevelExtensionOrNull: KotlinBaseExtension?
    get() = extensions.findByName(KOTLIN_PROJECT_EXTENSION_NAME)?.castIsolatedKotlinPluginClassLoaderAware<KotlinBaseExtension>()

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

internal fun ExplicitApiMode.toCompilerValue() = when (this) {
    ExplicitApiMode.Strict -> "strict"
    ExplicitApiMode.Warning -> "warning"
    ExplicitApiMode.Disabled -> "disable"
}

internal fun KotlinBaseExtension.explicitApiModeAsCompilerArg(): String? {
    val cliOption = explicitApi?.toCompilerValue()

    return cliOption?.let { "-Xexplicit-api=$it" }
}

@KotlinGradlePluginPublicDsl
abstract class KotlinProjectExtension @Inject constructor(
    override val project: Project
) : KotlinBaseExtension,
    HasMutableExtras,
    HasProject,
    ExtensionAware {

    override lateinit var coreLibrariesVersion: String

    final override val extras: MutableExtras = mutableExtrasOf()

    private val sourceSetsContainer = project.objects.domainObjectContainer(
        KotlinSourceSet::class.java,
        DefaultKotlinSourceSetFactory(project)
    ).also { kotlinSourceSets ->
        // Required for Gradle to generate accessors to source sets or 'sourceSets {}' DSL
        extensions.add("sourceSets", kotlinSourceSets)
    }
    override var sourceSets: NamedDomainObjectContainer<KotlinSourceSet>
        get() = sourceSetsContainer
        @Deprecated("Assigning new value to 'sourceSets' is deprecated", level = DeprecationLevel.ERROR)
        internal set(_) {
        }

    internal suspend fun awaitSourceSets(): NamedDomainObjectContainer<KotlinSourceSet> {
        KotlinPluginLifecycle.Stage.AfterFinaliseRefinesEdges.await()
        return sourceSets
    }

    private val toolchainSupport = ToolchainSupport.createToolchain(project)

    override fun jvmToolchain(action: Action<JavaToolchainSpec>) {
        toolchainSupport.applyToolchain(action)
    }

    override fun jvmToolchain(jdkVersion: Int) {
        jvmToolchain {
            it.languageVersion.set(JavaLanguageVersion.of(jdkVersion))
        }
    }

    @ExperimentalKotlinGradlePluginApi
    @get:JvmSynthetic
    override var kotlinDaemonJvmArgs: List<String>
        @Deprecated("", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException("It is not possible to get project wide kotlin daemon JVM args")
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

    @ExperimentalKotlinGradlePluginApi
    override fun <T : Named> NamedDomainObjectContainer<T>.invokeWhenCreated(name: String, configure: T.() -> Unit) {
        configureEach { if (it.name == name) it.configure() }
        project.launchInStage(KotlinPluginLifecycle.Stage.ReadyForExecution) {
            if (name !in names) {
                /* Expect 'named' to throw corresponding exception */
                named(name).configure(configure)
            }
        }
    }

    @ExperimentalKotlinGradlePluginApi
    @ExperimentalBuildToolsApi
    override val compilerVersion: Property<String> =
        project.objects.propertyWithConvention(project.getKotlinPluginVersion()).chainedFinalizeValueOnRead()
}

abstract class KotlinSingleTargetExtension<TARGET : KotlinTarget>(project: Project) : KotlinProjectExtension(project) {
    abstract val target: TARGET
    internal abstract val targetFuture: Future<TARGET>
    fun target(body: Action<TARGET>) = body.execute(target)
}

abstract class KotlinSingleJavaTargetExtension(project: Project) : KotlinSingleTargetExtension<KotlinWithJavaTarget<*, *>>(project)

abstract class KotlinJvmProjectExtension @Inject constructor(
    project: Project
) : KotlinSingleJavaTargetExtension(project),
    KotlinJvmExtension {
    @Suppress("DEPRECATION")
    override val target: KotlinWithJavaTarget<KotlinJvmOptions, KotlinJvmCompilerOptions>
        get() = targetFuture.getOrThrow()

    @Suppress("DEPRECATION")
    override val targetFuture = CompletableFuture<KotlinWithJavaTarget<KotlinJvmOptions, KotlinJvmCompilerOptions>>()

    open fun target(
        @Suppress("DEPRECATION") body: KotlinWithJavaTarget<KotlinJvmOptions, KotlinJvmCompilerOptions>.() -> Unit
    ) {
        project.launch(Undispatched) { targetFuture.await().body() }
    }

    override val compilerOptions: KotlinJvmCompilerOptions = project.objects.KotlinJvmCompilerOptionsDefault(project)

    override fun compilerOptions(configure: Action<KotlinJvmCompilerOptions>) {
        configure.execute(compilerOptions)
    }

    override fun compilerOptions(configure: KotlinJvmCompilerOptions.() -> Unit) {
        configure(compilerOptions)
    }
}

abstract class Kotlin2JsProjectExtension(project: Project) : KotlinSingleJavaTargetExtension(project) {
    @Suppress("DEPRECATION")
    override val target: KotlinWithJavaTarget<KotlinJsOptions, KotlinJsCompilerOptions>
        get() {
            if (!targetFuture.isCompleted) throw IllegalStateException("Extension target is not initialized!")
            return targetFuture.getOrThrow()
        }

    @Suppress("DEPRECATION")
    override val targetFuture = CompletableFuture<KotlinWithJavaTarget<KotlinJsOptions, KotlinJsCompilerOptions>>()
    open fun target(
        @Suppress("DEPRECATION") body: KotlinWithJavaTarget<KotlinJsOptions, KotlinJsCompilerOptions>.() -> Unit
    ) {
        project.launch(Undispatched) { targetFuture.await().body() }
    }
}

abstract class KotlinJsProjectExtension(project: Project) :
    KotlinSingleTargetExtension<KotlinJsTargetDsl>(project),
    KotlinJsCompilerTypeHolder {
    lateinit var irPreset: KotlinJsIrSingleTargetPreset

    @Deprecated("Use js() instead", ReplaceWith("js()"))
    override val target: KotlinJsTargetDsl
        get() = targetFuture.lenient.getOrNull() ?: js()

    @Deprecated("Because only IR compiler is left, no more necessary to know about compiler type in properties")
    override val compilerTypeFromProperties: KotlinJsCompilerType? = null

    override val targetFuture = CompletableFuture<KotlinJsTargetDsl>()

    fun registerTargetObserver(observer: (KotlinJsTargetDsl?) -> Unit) {
        project.launch(Undispatched) {
            observer(targetFuture.await())
        }
    }

    @Suppress("DEPRECATION")
    private fun jsInternal(
        body: KotlinJsTargetDsl.() -> Unit,
    ): KotlinJsTargetDsl {
        if (!targetFuture.isCompleted) {
            val target: KotlinJsTargetDsl = irPreset
                .createTargetInternal("js")

            this.targetFuture.complete(target)

            target.project.components.addAll(target.components)
        }

        target.run(body)

        return target
    }

    fun js(
        @Suppress("UNUSED_PARAMETER") // KT-64275
        compiler: KotlinJsCompilerType = defaultJsCompilerType,
        body: KotlinJsTargetDsl.() -> Unit = { },
    ): KotlinJsTargetDsl = jsInternal(body)

    fun js(
        compiler: String,
        body: KotlinJsTargetDsl.() -> Unit = { },
    ): KotlinJsTargetDsl = js(
        KotlinJsCompilerType.byArgument(compiler),
        body
    )

    fun js(
        body: KotlinJsTargetDsl.() -> Unit = { },
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
    fun getTargets(): NamedDomainObjectContainer<KotlinTarget>? =
        targetFuture.lenient.getOrNull()?.let { target ->
            target.project.container(KotlinTarget::class.java)
                .apply { add(target) }
        }
}

abstract class KotlinCommonProjectExtension(project: Project) : KotlinSingleJavaTargetExtension(project) {
    override val target: KotlinWithJavaTarget<*, *> get() = targetFuture.getOrThrow()

    @Suppress("DEPRECATION")
    override val targetFuture =
        CompletableFuture<KotlinWithJavaTarget<KotlinMultiplatformCommonOptions, KotlinMultiplatformCommonCompilerOptions>>()

    open fun target(
        @Suppress("DEPRECATION")
        body: KotlinWithJavaTarget<KotlinMultiplatformCommonOptions, KotlinMultiplatformCommonCompilerOptions>.() -> Unit,
    ) = project.launch(Undispatched) {
        targetFuture.await().body()
    }
}

abstract class KotlinAndroidProjectExtension @Inject constructor(
    project: Project
) : KotlinSingleTargetExtension<KotlinAndroidTarget>(project),
    KotlinAndroidExtension {
    override val target: KotlinAndroidTarget get() = targetFuture.getOrThrow()
    override val targetFuture = CompletableFuture<KotlinAndroidTarget>()

    open fun target(body: KotlinAndroidTarget.() -> Unit) = project.launch(Undispatched) {
        targetFuture.await().body()
    }

    override val compilerOptions: KotlinJvmCompilerOptions = project.objects.KotlinJvmCompilerOptionsDefault(project)

    override fun compilerOptions(configure: Action<KotlinJvmCompilerOptions>) {
        configure.execute(compilerOptions)
    }

    override fun compilerOptions(configure: KotlinJvmCompilerOptions.() -> Unit) {
        configure(compilerOptions)
    }
}

enum class NativeCacheKind(val produce: String?, val outputKind: CompilerOutputKind?) {
    NONE(null, null),
    DYNAMIC("dynamic_cache", CompilerOutputKind.DYNAMIC_CACHE),
    STATIC("static_cache", CompilerOutputKind.STATIC_CACHE),
    HEADER("header_cache", CompilerOutputKind.HEADER_CACHE);

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
