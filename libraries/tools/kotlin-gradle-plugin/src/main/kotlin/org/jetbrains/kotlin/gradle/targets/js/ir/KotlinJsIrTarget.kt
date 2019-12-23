/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.AbstractKotlinTargetConfigurator.Companion.runTaskNameSuffix
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetWithTests
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTarget
import org.jetbrains.kotlin.gradle.targets.js.JsIrAggregatingExecutionSource
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsIrReportAggregatingTestRun
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsIrBrowserDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsIrNodeDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsIrTargetDsl
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.testing.internal.KotlinTestReport
import org.jetbrains.kotlin.gradle.testing.testTaskName
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import javax.inject.Inject

open class KotlinJsIrTarget @Inject constructor(project: Project, platformType: KotlinPlatformType) :
    KotlinOnlyTarget<KotlinJsCompilation>(project, platformType),
    KotlinTargetWithTests<JsIrAggregatingExecutionSource, KotlinJsIrReportAggregatingTestRun>,
    KotlinJsIrTargetDsl {
    override lateinit var testRuns: NamedDomainObjectContainer<KotlinJsIrReportAggregatingTestRun>
        internal set

    val testTaskName get() = testRuns.getByName(KotlinTargetWithTests.DEFAULT_TEST_RUN_NAME).testTaskName
    val testTask: TaskProvider<KotlinTestReport>
        get() = checkNotNull(project.locateTask(testTaskName))

    val runTaskName get() = lowerCamelCaseName(disambiguationClassifier, runTaskNameSuffix)
    val runTask
        get() = project.tasks.maybeCreate(runTaskName).also {
            it.description = "Run js on all configured platforms"
        }

    private val browserLazyDelegate = lazy {
        project.objects.newInstance(KotlinBrowserJsIr::class.java, this).also {
            it.configure()
            browserConfiguredHandlers.forEach { handler -> handler(it) }
            browserConfiguredHandlers.clear()
        }
    }

    private val browserConfiguredHandlers = mutableListOf<KotlinJsIrBrowserDsl.() -> Unit>()

    val browser by browserLazyDelegate

    internal val isBrowserConfigured: Boolean = browserLazyDelegate.isInitialized()

    override fun browser(body: KotlinJsIrBrowserDsl.() -> Unit) {
        body(browser)
    }

    private val nodejsLazyDelegate = lazy {
        project.objects.newInstance(KotlinNodeJsIr::class.java, this).also {
            it.configure()
            nodejsConfiguredHandlers.forEach { handler -> handler(it) }
            nodejsConfiguredHandlers.clear()
        }
    }

    private val nodejsConfiguredHandlers = mutableListOf<KotlinJsIrNodeDsl.() -> Unit>()

    val nodejs by nodejsLazyDelegate

    internal val isNodejsConfigured: Boolean = nodejsLazyDelegate.isInitialized()

    override fun nodejs(body: KotlinJsIrNodeDsl.() -> Unit) {
        body(nodejs)
    }

    fun whenBrowserConfigured(body: KotlinJsIrBrowserDsl.() -> Unit) {
        if (browserLazyDelegate.isInitialized()) {
            browser(body)
        } else {
            browserConfiguredHandlers += body
        }
    }

    fun whenNodejsConfigured(body: KotlinJsIrNodeDsl.() -> Unit) {
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
}