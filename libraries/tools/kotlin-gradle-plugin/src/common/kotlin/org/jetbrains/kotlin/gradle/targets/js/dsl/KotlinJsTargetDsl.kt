/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dsl

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectContainer
import org.jetbrains.kotlin.gradle.dsl.HasConfigurableCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJsDce
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.HasBinaries
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsPlatformTestRun
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsReportAggregatingTestRun
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsBinaryContainer
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

interface KotlinJsSubTargetContainerDsl : KotlinTarget {
    val nodejs: KotlinJsNodeDsl

    val browser: KotlinJsBrowserDsl

    val isNodejsConfigured: Boolean

    val isBrowserConfigured: Boolean

    fun whenNodejsConfigured(body: KotlinJsNodeDsl.() -> Unit)

    fun whenBrowserConfigured(body: KotlinJsBrowserDsl.() -> Unit)
}

interface KotlinJsTargetDsl :
    KotlinTarget,
    KotlinTargetWithNodeJsDsl,
    HasBinaries<KotlinJsBinaryContainer>,
    HasConfigurableCompilerOptions<KotlinJsCompilerOptions> {

    var moduleName: String?

    fun browser() = browser { }
    fun browser(body: KotlinJsBrowserDsl.() -> Unit)
    fun browser(fn: Action<KotlinJsBrowserDsl>) {
        browser {
            fn.execute(this)
        }
    }

    fun useCommonJs()
    fun useEsModules()

    /**
     * The function accepts [jsExpression] and puts this expression as the "args: Array<String>" argument in place of main-function call
     */
    @ExperimentalMainFunctionArgumentsDsl
    fun passAsArgumentToMainFunction(jsExpression: String)

    fun generateTypeScriptDefinitions()

    @Deprecated(
        message = "produceExecutable() was changed on binaries.executable()",
        replaceWith = ReplaceWith("binaries.executable()"),
        level = DeprecationLevel.ERROR
    )
    fun produceExecutable() {
        throw GradleException("Please change produceExecutable() on binaries.executable()")
    }

    val testRuns: NamedDomainObjectContainer<KotlinJsReportAggregatingTestRun>

    // Need to compatibility when users use KotlinJsCompilation specific in build script
    override val compilations: NamedDomainObjectContainer<KotlinJsIrCompilation>

    override val binaries: KotlinJsBinaryContainer
}

interface KotlinTargetWithNodeJsDsl {
    fun nodejs() = nodejs { }
    fun nodejs(body: KotlinJsNodeDsl.() -> Unit)
    fun nodejs(fn: Action<KotlinJsNodeDsl>) {
        nodejs {
            fn.execute(this)
        }
    }
}

interface KotlinJsSubTargetDsl {
    @ExperimentalDistributionDsl
    fun distribution(body: Action<Distribution>)

    fun testTask(body: Action<KotlinJsTest>)

    val testRuns: NamedDomainObjectContainer<KotlinJsPlatformTestRun>
}

interface KotlinJsBrowserDsl : KotlinJsSubTargetDsl {
    fun commonWebpackConfig(body: Action<KotlinWebpackConfig>)

    fun runTask(body: Action<KotlinWebpack>)

    fun webpackTask(body: Action<KotlinWebpack>)

    @ExperimentalDceDsl
    fun dceTask(body: Action<KotlinJsDce>)
}

interface KotlinJsNodeDsl : KotlinJsSubTargetDsl {
    fun runTask(body: Action<NodeJsExec>)

    @ExperimentalMainFunctionArgumentsDsl
    fun passProcessArgvToMainFunction()
}