/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import groovy.lang.Closure
import org.jetbrains.kotlin.gradle.execution.KotlinAggregateExecutionSource
import org.jetbrains.kotlin.gradle.plugin.CompilationExecutionSource
import org.jetbrains.kotlin.gradle.plugin.CompilationExecutionSourceSupport
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetTestRun
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsSubTargetContainerDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsSubTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.testing.KotlinReportAggregatingTestRun
import org.jetbrains.kotlin.gradle.testing.KotlinTaskTestRun
import org.jetbrains.kotlin.gradle.testing.requireCompilationOfTarget
import javax.inject.Inject
import kotlin.properties.Delegates

class JsCompilationExecutionSource(override val compilation: KotlinJsIrCompilation) :
    CompilationExecutionSource<KotlinJsIrCompilation>

open class KotlinJsPlatformTestRun(testRunName: String, target: KotlinTarget) :
    KotlinTaskTestRun<JsCompilationExecutionSource, KotlinJsTest>(testRunName, target),
    CompilationExecutionSourceSupport<KotlinJsIrCompilation> {

    private var _executionSource: JsCompilationExecutionSource by Delegates.notNull()

    final override var executionSource: JsCompilationExecutionSource
        get() = _executionSource
        set(value) {
            executionTask.configure { it.compilation = value.compilation }
            _executionSource = value
        }

    override fun setExecutionSourceFrom(compilation: KotlinJsIrCompilation) {
        requireCompilationOfTarget(compilation, target)

        executionSource = JsCompilationExecutionSource(compilation)
    }
}

class JsAggregatingExecutionSource(private val aggregatingTestRun: KotlinJsReportAggregatingTestRun) :
    KotlinAggregateExecutionSource<JsCompilationExecutionSource> {

    override val executionSources: Iterable<JsCompilationExecutionSource>
        get() = aggregatingTestRun.getConfiguredExecutions().map { it.executionSource }
}

abstract class KotlinJsReportAggregatingTestRun @Inject constructor(
    testRunName: String,
    override val target: KotlinJsSubTargetContainerDsl,
) : KotlinReportAggregatingTestRun<JsCompilationExecutionSource, JsAggregatingExecutionSource, KotlinJsPlatformTestRun>(testRunName),
    KotlinTargetTestRun<JsAggregatingExecutionSource>,
    CompilationExecutionSourceSupport<KotlinJsIrCompilation> {

    override fun setExecutionSourceFrom(compilation: KotlinJsIrCompilation) = configureAllExecutions {
        setExecutionSourceFrom(compilation)
    }

    override val executionSource: JsAggregatingExecutionSource
        get() = JsAggregatingExecutionSource(this)

    private fun KotlinJsSubTargetDsl.getChildTestExecution() = testRuns.maybeCreate(testRunName)

    override fun getConfiguredExecutions(): Iterable<KotlinJsPlatformTestRun> = mutableListOf<KotlinJsPlatformTestRun>().apply {
        target.subTargets
            .configureEach { subTarget ->
                add(subTarget.getChildTestExecution())
            }
    }

    override fun configureAllExecutions(configure: KotlinJsPlatformTestRun.() -> Unit) {
        val doConfigureInChildren: KotlinJsSubTargetDsl.() -> Unit = {
            configure(getChildTestExecution())
        }

        target.subTargets.configureEach { subTarget ->
            doConfigureInChildren(subTarget)
        }
    }

    override fun filter(configureFilter: Closure<*>) = filter { target.project.configure(this, configureFilter) }
}