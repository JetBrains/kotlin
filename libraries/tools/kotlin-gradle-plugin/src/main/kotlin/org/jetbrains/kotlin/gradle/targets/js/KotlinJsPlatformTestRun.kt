/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.jetbrains.kotlin.gradle.execution.KotlinAggregateExecutionSource
import org.jetbrains.kotlin.gradle.plugin.CompilationExecutionSource
import org.jetbrains.kotlin.gradle.plugin.CompilationExecutionSourceSupport
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetTestRun
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.subtargets.KotlinJsSubTarget
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.testing.KotlinReportAggregatingTestRun
import org.jetbrains.kotlin.gradle.testing.KotlinTaskTestRun
import org.jetbrains.kotlin.gradle.testing.requireCompilationOfTarget
import kotlin.properties.Delegates

class JsCompilationExecutionSource(override val compilation: KotlinJsCompilation) :
    CompilationExecutionSource<KotlinJsCompilation>

open class KotlinJsPlatformTestRun(testRunName: String, subtarget: KotlinJsSubTarget) :
    KotlinTaskTestRun<JsCompilationExecutionSource, KotlinJsTest>(testRunName, subtarget.target),
    CompilationExecutionSourceSupport<KotlinJsCompilation> {

    private var _executionSource: JsCompilationExecutionSource by Delegates.notNull()

    final override var executionSource: JsCompilationExecutionSource
        get() = _executionSource
        set(value) {
            executionTask.configure { it.compilation = value.compilation }
            _executionSource = value
        }

    override fun setExecutionSourceFrom(compilation: KotlinJsCompilation) {
        requireCompilationOfTarget(compilation, target)

        executionSource = JsCompilationExecutionSource(compilation)
    }
}

class JsAggregatingExecutionSource(private val aggregatingTestRun: KotlinJsReportAggregatingTestRun) :
    KotlinAggregateExecutionSource<JsCompilationExecutionSource> {

    override val executionSources: Iterable<JsCompilationExecutionSource>
        get() = aggregatingTestRun.getConfiguredExecutions().map { it.executionSource }
}

class JsIrAggregatingExecutionSource(private val aggregatingTestRun: KotlinJsIrReportAggregatingTestRun) :
    KotlinAggregateExecutionSource<JsCompilationExecutionSource> {

    override val executionSources: Iterable<JsCompilationExecutionSource>
        get() = aggregatingTestRun.getConfiguredExecutions().map { it.executionSource }
}

open class KotlinJsReportAggregatingTestRun(
    testRunName: String,
    override val target: KotlinJsTarget
) : KotlinReportAggregatingTestRun<JsCompilationExecutionSource, JsAggregatingExecutionSource, KotlinJsPlatformTestRun>(testRunName),
    KotlinTargetTestRun<JsAggregatingExecutionSource>,
    CompilationExecutionSourceSupport<KotlinJsCompilation> {

    override fun setExecutionSourceFrom(compilation: KotlinJsCompilation) = configureAllExecutions {
        setExecutionSourceFrom(compilation)
    }

    override val executionSource: JsAggregatingExecutionSource
        get() = JsAggregatingExecutionSource(this)

    private fun KotlinJsSubTarget.getChildTestExecution() = testRuns.maybeCreate(testRunName)

    override fun getConfiguredExecutions(): Iterable<KotlinJsPlatformTestRun> = mutableListOf<KotlinJsPlatformTestRun>().apply {
        if (target.isNodejsConfigured) {
            add(target.nodejs.getChildTestExecution())
        }
        if (target.isBrowserConfigured) {
            add(target.browser.getChildTestExecution())
        }
    }

    override fun configureAllExecutions(configure: KotlinJsPlatformTestRun.() -> Unit) {
        val doConfigureInChildren: KotlinJsSubTarget.() -> Unit = {
            configure(getChildTestExecution())
        }

        target.whenBrowserConfigured { doConfigureInChildren(this as KotlinJsSubTarget) }
        target.whenNodejsConfigured { doConfigureInChildren(this as KotlinJsSubTarget) }
    }
}

open class KotlinJsIrReportAggregatingTestRun(
    testRunName: String,
    override val target: KotlinJsIrTarget
) : KotlinReportAggregatingTestRun<JsCompilationExecutionSource, JsIrAggregatingExecutionSource, KotlinJsPlatformTestRun>(testRunName),
    KotlinTargetTestRun<JsIrAggregatingExecutionSource>,
    CompilationExecutionSourceSupport<KotlinJsCompilation> {

    override fun setExecutionSourceFrom(compilation: KotlinJsCompilation) = configureAllExecutions {
        setExecutionSourceFrom(compilation)
    }

    override val executionSource: JsIrAggregatingExecutionSource
        get() = JsIrAggregatingExecutionSource(this)

    private fun KotlinJsSubTarget.getChildTestExecution() = testRuns.maybeCreate(testRunName)

    override fun getConfiguredExecutions(): Iterable<KotlinJsPlatformTestRun> = mutableListOf<KotlinJsPlatformTestRun>().apply {
        if (target.isNodejsConfigured) {
            add(target.nodejs.getChildTestExecution())
        }
        if (target.isBrowserConfigured) {
            add(target.browser.getChildTestExecution())
        }
    }

    override fun configureAllExecutions(configure: KotlinJsPlatformTestRun.() -> Unit) {
        val doConfigureInChildren: KotlinJsSubTarget.() -> Unit = {
            configure(getChildTestExecution())
        }

        target.whenBrowserConfigured { doConfigureInChildren(this as KotlinJsSubTarget) }
        target.whenNodejsConfigured { doConfigureInChildren(this as KotlinJsSubTarget) }
    }
}