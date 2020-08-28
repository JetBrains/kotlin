/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import org.jetbrains.kotlin.gradle.targets.js.JsAggregatingExecutionSource
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsReportAggregatingTestRun
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBrowserDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsNodeDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsSubTargetContainerDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
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
    KotlinJsSubTargetContainerDsl {
    override lateinit var testRuns: NamedDomainObjectContainer<KotlinJsReportAggregatingTestRun>
        internal set

    open var isMpp: Boolean? = null
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
                    overrideConfigurationArtifacts = emptySet()
                )
    }

    internal val commonFakeApiElementsConfigurationName: String
        get() = disambiguateName("commonFakeApiElements")

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
    val runTask: Task
        get() = project.tasks.maybeCreate(runTaskName).also {
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
                project.layout.file(binary.linkTask.map { it.destinationDir })
            )

            task.from(project.tasks.named(compilation.processResourcesTaskName))

            task.into(
                npmProject.dist
            )
        }
    }

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

    private fun KotlinJsIrSubTarget.configureSubTarget() {
        configureTestSideEffect
        configure()
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
        sourceMapEmbedSources = null
    }
}