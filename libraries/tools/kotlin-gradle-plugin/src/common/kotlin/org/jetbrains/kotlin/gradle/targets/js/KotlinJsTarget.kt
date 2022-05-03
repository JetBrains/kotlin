/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.AbstractKotlinTargetConfigurator.Companion.runTaskNameSuffix
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType.LEGACY
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.js.dsl.*
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsBinaryContainer
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.subtargets.KotlinBrowserJs
import org.jetbrains.kotlin.gradle.targets.js.subtargets.KotlinNodeJs
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.testing.internal.KotlinTestReport
import org.jetbrains.kotlin.gradle.testing.testTaskName
import org.jetbrains.kotlin.gradle.utils.dashSeparatedName
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.setProperty
import javax.inject.Inject

open class KotlinJsTarget
@Inject
constructor(
    project: Project,
    platformType: KotlinPlatformType
) :
    KotlinTargetWithBinaries<KotlinJsCompilation, KotlinJsBinaryContainer>(project, platformType),
    KotlinTargetWithTests<JsAggregatingExecutionSource, KotlinJsReportAggregatingTestRun>,
    KotlinJsTargetDsl,
    KotlinJsSubTargetContainerDsl {
    override lateinit var testRuns: NamedDomainObjectContainer<KotlinJsReportAggregatingTestRun>
        internal set

    override var moduleName: String? = null
        set(value) {
            check(!isBrowserConfigured && !isNodejsConfigured) {
                "Please set moduleName before initialize browser() or nodejs()"
            }
            field = value
        }

    internal val commonFakeApiElementsConfigurationName: String
        get() = lowerCamelCaseName(
            irTarget?.let {
                this.disambiguationClassifierInPlatform
            } ?: disambiguationClassifier,
            "commonFakeApiElements"
        )

    val disambiguationClassifierInPlatform: String?
        get() = if (irTarget != null) {
            disambiguationClassifier?.removeJsCompilerSuffix(LEGACY)
        } else {
            disambiguationClassifier
        }

    override val kotlinComponents: Set<KotlinTargetComponent> by lazy {
        if (irTarget == null)
            super.kotlinComponents
        else {
            val mainCompilation = compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
            val usageContexts = createUsageContexts(mainCompilation) +
                    irTarget!!.createUsageContexts(irTarget!!.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME))

            val componentName =
                if (project.kotlinExtension is KotlinMultiplatformExtension)
                    irTarget?.let { targetName.removeJsCompilerSuffix(LEGACY) } ?: targetName
                else PRIMARY_SINGLE_COMPONENT_NAME

            val result = createKotlinVariant(componentName, mainCompilation, usageContexts)

            result.sourcesArtifacts = setOf(
                sourcesJarArtifact(mainCompilation, componentName, dashSeparatedName(targetName.toLowerCase()))
            )

            setOf(result)
        }
    }

    override fun createUsageContexts(producingCompilation: KotlinCompilation<*>): Set<DefaultKotlinUsageContext> {
        val usageContexts = super.createUsageContexts(producingCompilation)

        if (isMpp!!) return usageContexts

        return usageContexts +
                DefaultKotlinUsageContext(
                    compilation = compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME),
                    usage = project.usageByName("java-api-jars"),
                    dependencyConfigurationName = commonFakeApiElementsConfigurationName,
                    overrideConfigurationArtifacts = project.setProperty { emptyList() }
                )
    }

    override fun createKotlinVariant(
        componentName: String,
        compilation: KotlinCompilation<*>,
        usageContexts: Set<DefaultKotlinUsageContext>
    ): KotlinVariant {
        return super.createKotlinVariant(componentName, compilation, usageContexts).apply {
            irTarget?.let {
                artifactTargetName = targetName.removeJsCompilerSuffix(LEGACY)
            }
        }
    }

    override val binaries: KotlinJsBinaryContainer
        get() = compilations.withType(KotlinJsCompilation::class.java)
            .named(MAIN_COMPILATION_NAME)
            .map { it.binaries }
            .get()

    var irTarget: KotlinJsIrTarget? = null
        internal set

    open var isMpp: Boolean? = null
        internal set

    val testTaskName get() = testRuns.getByName(KotlinTargetWithTests.DEFAULT_TEST_RUN_NAME).testTaskName
    val testTask: TaskProvider<KotlinTestReport>
        get() = checkNotNull(project.locateTask(testTaskName))

    val runTaskName get() = lowerCamelCaseName(disambiguationClassifier, runTaskNameSuffix)
    val runTask: TaskProvider<Task>
        get() = project.locateOrRegisterTask(runTaskName) {
            it.description = "Run js on all configured platforms"
        }

    private val propertiesProvider = PropertiesProvider(project)

    //Browser
    private val browserLazyDelegate = lazy {
        project.objects.newInstance(KotlinBrowserJs::class.java, this).also {
            it.configure()

            if (propertiesProvider.jsGenerateExecutableDefault && irTarget == null) {
                binaries.executable()
            }

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
        irTarget?.browser(body)
    }

    //node.js
    private val nodejsLazyDelegate = lazy {
        project.objects.newInstance(KotlinNodeJs::class.java, this).also {
            it.configure()

            if (propertiesProvider.jsGenerateExecutableDefault && irTarget == null) {
                binaries.executable()
            }

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
        irTarget?.nodejs(body)
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
            it.kotlinOptions {
                moduleKind = "commonjs"
                sourceMap = true
                sourceMapEmbedSources = null
            }
        }
        irTarget?.useCommonJs()
    }
}