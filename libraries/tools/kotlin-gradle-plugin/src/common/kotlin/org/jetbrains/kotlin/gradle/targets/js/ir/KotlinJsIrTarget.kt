/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinJsOptions
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.AbstractKotlinTargetConfigurator.Companion.runTaskNameSuffix
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultKotlinUsageContext
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinTargetWithBinaries
import org.jetbrains.kotlin.gradle.targets.js.JsAggregatingExecutionSource
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsReportAggregatingTestRun
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.*
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.setProperty
import javax.inject.Inject

open class KotlinJsIrTarget
@Inject
constructor(
    project: Project,
    platformType: KotlinPlatformType,
    internal val mixedMode: Boolean
) :
    KotlinTargetWithBinaries<KotlinJsIrCompilation, KotlinJsBinaryContainer>(project, platformType),
    KotlinTargetWithTests<JsAggregatingExecutionSource, KotlinJsReportAggregatingTestRun>,
    KotlinJsTargetDsl,
    KotlinWasmTargetDsl,
    KotlinJsSubTargetContainerDsl,
    KotlinWasmSubTargetContainerDsl {
    override lateinit var testRuns: NamedDomainObjectContainer<KotlinJsReportAggregatingTestRun>
        internal set

    open var isMpp: Boolean? = null
        internal set

    var legacyTarget: KotlinJsTarget? = null
        internal set

    override var moduleName: String? = null
        set(value) {
            check(!isBrowserConfigured && !isNodejsConfigured) {
                "Please set moduleName before initialize browser() or nodejs()"
            }
            field = value
        }

    override fun createUsageContexts(producingCompilation: KotlinCompilation<*>): Set<DefaultKotlinUsageContext> {
        val usageContexts = super.createUsageContexts(producingCompilation)

        if (isMpp!! || mixedMode) return usageContexts

        return usageContexts +
                DefaultKotlinUsageContext(
                    compilation = compilations.getByName(MAIN_COMPILATION_NAME),
                    usage = project.usageByName("java-api-jars"),
                    dependencyConfigurationName = commonFakeApiElementsConfigurationName,
                    overrideConfigurationArtifacts = project.setProperty { emptyList() }
                )
    }

    internal val commonFakeApiElementsConfigurationName: String
        get() = lowerCamelCaseName(
            if (mixedMode)
                disambiguationClassifierInPlatform
            else
                disambiguationClassifier,
            "commonFakeApiElements"
        )

    val disambiguationClassifierInPlatform: String?
        get() = if (mixedMode) {
            disambiguationClassifier?.removeJsCompilerSuffix(KotlinJsCompilerType.IR)
        } else {
            disambiguationClassifier
        }

    override val binaries: KotlinJsBinaryContainer
        get() = compilations.withType(KotlinJsIrCompilation::class.java)
            .named(MAIN_COMPILATION_NAME)
            .map { it.binaries }
            .get()

    private val runTaskName get() = lowerCamelCaseName(disambiguationClassifier, runTaskNameSuffix)
    val runTask: TaskProvider<Task>
        get() = project.locateOrRegisterTask(runTaskName) {
            it.description = "Run js on all configured platforms"
        }

    private val configureTestSideEffect: Unit by lazy {
        compilations.matching { it.name == KotlinCompilation.TEST_COMPILATION_NAME }
            .all { compilation ->
                compilation.binaries.executableIrInternal(compilation)
            }
    }

    private val commonLazy by lazy {
        compilations.all { compilation ->
            val npmProject = compilation.npmProject
            compilation.binaries
                .withType(JsIrBinary::class.java)
                .all { binary ->
                    val syncTask = registerCompileSync(binary)

                    binary.linkTask.configure {
                        it.kotlinOptions.outputFile = project.buildDir
                            .resolve(COMPILE_SYNC)
                            .resolve(compilation.name)
                            .resolve(binary.name)
                            .resolve(npmProject.main)
                            .canonicalPath

                        it.finalizedBy(syncTask)
                    }
                }
        }
    }

    private fun registerCompileSync(binary: JsIrBinary): TaskProvider<Copy> {
        val compilation = binary.compilation
        val npmProject = compilation.npmProject
        return project.registerTask(
            binary.linkSyncTaskName
        ) { task ->
            task.from(
                project.layout.file(
                    binary.linkTask.flatMap { linkTask ->
                        linkTask.normalizedDestinationDirectory.map { it.asFile }
                    }
                )
            )

            task.from(project.tasks.named(compilation.processResourcesTaskName))

            task.into(
                npmProject.dist
            )
        }
    }

    //Browser
    private val browserLazyDelegate = lazy {
        commonLazy
        project.objects.newInstance(KotlinBrowserJsIr::class.java, this).also {
            it.configureSubTarget()
            browserConfiguredHandlers.forEach { handler ->
                handler(it)
            }
            browserConfiguredHandlers.clear()
        }
    }

    private val browserConfiguredHandlers = mutableListOf<KotlinJsBrowserDsl.() -> Unit>()

    override val browser by browserLazyDelegate

    override val isBrowserConfigured: Boolean
        get() = browserLazyDelegate.isInitialized()

    override fun browser(body: KotlinJsBrowserDsl.() -> Unit) {
        body(browser)
    }

    //node.js
    private val nodejsLazyDelegate = lazy {
        commonLazy
        project.objects.newInstance(KotlinNodeJsIr::class.java, this).also {
            it.configureSubTarget()
            nodejsConfiguredHandlers.forEach { handler ->
                handler(it)
            }

            nodejsConfiguredHandlers.clear()
        }
    }

    private val nodejsConfiguredHandlers = mutableListOf<KotlinJsNodeDsl.() -> Unit>()

    override val nodejs by nodejsLazyDelegate

    override val isNodejsConfigured: Boolean
        get() = nodejsLazyDelegate.isInitialized()

    override fun nodejs(body: KotlinJsNodeDsl.() -> Unit) {
        body(nodejs)
    }

    //d8
    private val d8LazyDelegate = lazy {
        commonLazy
        project.objects.newInstance(KotlinD8Ir::class.java, this).also {
            it.configureSubTarget()
            d8ConfiguredHandlers.forEach { handler ->
                handler(it)
            }

            d8ConfiguredHandlers.clear()
        }
    }

    private val d8ConfiguredHandlers = mutableListOf<KotlinWasmD8Dsl.() -> Unit>()

    override val d8 by d8LazyDelegate

    override val isD8Configured: Boolean
        get() = d8LazyDelegate.isInitialized()

    private fun KotlinJsIrSubTarget.configureSubTarget() {
        configureTestSideEffect
        configure()
    }

    override fun d8(body: KotlinWasmD8Dsl.() -> Unit) {
        body(d8)
    }

    override fun whenBrowserConfigured(body: KotlinJsBrowserDsl.() -> Unit) {
        if (browserLazyDelegate.isInitialized()) {
            browser(body)
        } else {
            browserConfiguredHandlers += body
        }
    }

    override fun whenNodejsConfigured(body: KotlinJsNodeDsl.() -> Unit) {
        if (nodejsLazyDelegate.isInitialized()) {
            nodejs(body)
        } else {
            nodejsConfiguredHandlers += body
        }
    }

    override fun whenD8Configured(body: KotlinWasmD8Dsl.() -> Unit) {
        if (d8LazyDelegate.isInitialized()) {
            d8(body)
        } else {
            d8ConfiguredHandlers += body
        }
    }

    override fun useCommonJs() {
        compilations.all {
            it.kotlinOptions.configureCommonJsOptions()

            binaries
                .withType(JsIrBinary::class.java)
                .all {
                    it.linkTask.configure { linkTask ->
                        linkTask.kotlinOptions.configureCommonJsOptions()
                    }
                }
        }
    }

    private fun KotlinJsOptions.configureCommonJsOptions() {
        moduleKind = "commonjs"
        sourceMap = true
        sourceMapEmbedSources = "never"
    }
}
