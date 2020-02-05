/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributesSchema
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.AbstractKotlinTargetConfigurator.Companion.runTaskNameSuffix
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetComponent
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetWithTests
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.PRIMARY_SINGLE_COMPONENT_NAME
import org.jetbrains.kotlin.gradle.targets.js.dsl.*
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.subtargets.KotlinBrowserJs
import org.jetbrains.kotlin.gradle.targets.js.subtargets.KotlinJsSubTarget
import org.jetbrains.kotlin.gradle.targets.js.subtargets.KotlinNodeJs
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.testing.internal.KotlinTestReport
import org.jetbrains.kotlin.gradle.testing.testTaskName
import org.jetbrains.kotlin.gradle.utils.dashSeparatedName
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import javax.inject.Inject

open class KotlinJsTarget
@Inject
constructor(
    project: Project,
    platformType: KotlinPlatformType
) :
    KotlinOnlyTarget<KotlinJsCompilation>(project, platformType),
    KotlinTargetWithTests<JsAggregatingExecutionSource, KotlinJsReportAggregatingTestRun>,
    KotlinJsTargetDsl,
    KotlinJsSubTargetContainerDsl {
    override lateinit var testRuns: NamedDomainObjectContainer<KotlinJsReportAggregatingTestRun>
        internal set

    override val kotlinComponents: Set<KotlinTargetComponent> by lazy {
        if (irTarget == null)
            super.kotlinComponents
        else {
            val mainCompilation = compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
            val usageContexts = createUsageContexts(mainCompilation) +
                    irTarget!!.createUsageContexts(irTarget!!.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME))

            val componentName =
                if (project.kotlinExtension is KotlinMultiplatformExtension)
                    targetName
                else PRIMARY_SINGLE_COMPONENT_NAME

            val result = createKotlinVariant(componentName, mainCompilation, usageContexts)

            result.sourcesArtifacts = setOf(
                sourcesJarArtifact(mainCompilation, componentName, dashSeparatedName(targetName.toLowerCase()))
            )

            setOf(result)
        }
    }

    var producingType: KotlinJsProducingType? = null

    var irTarget: KotlinJsIrTarget? = null

    val testTaskName get() = testRuns.getByName(KotlinTargetWithTests.DEFAULT_TEST_RUN_NAME).testTaskName
    val testTask: TaskProvider<KotlinTestReport>
        get() = checkNotNull(project.locateTask(testTaskName))

    val runTaskName get() = lowerCamelCaseName(disambiguationClassifier, runTaskNameSuffix)
    val runTask
        get() = project.tasks.maybeCreate(runTaskName).also {
            it.description = "Run js on all configured platforms"
        }

    private val browserLazyDelegate = lazy {
        project.objects.newInstance(KotlinBrowserJs::class.java, this).also {
            it.configure()

            browserConfiguredHandlers.forEach { handler ->
                handler(it)
            }
            browserConfiguredHandlers.clear()
        }
    }

    private val browserConfiguredHandlers = mutableListOf<KotlinJsBrowserDsl.() -> Unit>()

    override val browser by browserLazyDelegate

    override val isBrowserConfigured: Boolean = browserLazyDelegate.isInitialized()

    override fun browser(body: KotlinJsBrowserDsl.() -> Unit) {
        body(browser)
        irTarget?.browser(body)
    }

    private val nodejsLazyDelegate = lazy {
        project.objects.newInstance(KotlinNodeJs::class.java, this).also {
            it.configure()
            nodejsConfiguredHandlers.forEach { handler ->
                handler(it)
            }

            nodejsConfiguredHandlers.clear()
        }
    }

    private val nodejsConfiguredHandlers = mutableListOf<KotlinJsNodeDsl.() -> Unit>()

    override val nodejs by nodejsLazyDelegate

    override val isNodejsConfigured: Boolean = nodejsLazyDelegate.isInitialized()

    override fun nodejs(body: KotlinJsNodeDsl.() -> Unit) {
        body(nodejs)
        irTarget?.nodejs(body)
    }

    override fun produceKotlinLibrary() {
        produce(KotlinJsProducingType.KOTLIN_LIBRARY)
    }

    override fun produceExecutable() {
        produce(KotlinJsProducingType.EXECUTABLE) {
            (this as KotlinJsSubTarget).produceExecutable()
        }
    }

    private fun produce(
        producingType: KotlinJsProducingType,
        producer: KotlinJsSubTargetDsl.() -> Unit = {}
    ) {
        check(this.producingType == null || this.producingType == producingType) {
            "Only one producing type supported. Try to set $producingType but previously ${this.producingType} found"
        }

        this.producingType = producingType

        whenBrowserConfigured {
            if (this is KotlinBrowserJs) {
                producer()
            }
        }

        whenNodejsConfigured {
            if (this is KotlinNodeJs) {
                producer()
            }
        }
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

    fun useCommonJs() {
        compilations.all {
            it.compileKotlinTask.kotlinOptions {
                moduleKind = "commonjs"
                sourceMap = true
                sourceMapEmbedSources = null
            }
        }
    }

    companion object {
        val jsCompilerAttribute = Attribute.of(
            "org.jetbrains.kotlin.js.compiler",
            JsCompilerType::class.java
        )

        fun setupAttributesMatchingStrategy(attributesSchema: AttributesSchema) {
            attributesSchema.attribute(jsCompilerAttribute)
        }
    }
}