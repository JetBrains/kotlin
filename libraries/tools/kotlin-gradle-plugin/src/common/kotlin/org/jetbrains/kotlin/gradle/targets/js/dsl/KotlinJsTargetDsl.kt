/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dsl

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectContainer
import org.jetbrains.kotlin.gradle.dsl.KotlinJsDce
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsPlatformTestRun
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsReportAggregatingTestRun
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsBinaryContainer
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

interface KotlinJsTargetDsl : KotlinTarget {
    var moduleName: String?

    fun browser() = browser { }
    fun browser(body: KotlinJsBrowserDsl.() -> Unit)
    fun browser(fn: Action<KotlinJsBrowserDsl>) {
        browser {
            fn.execute(this)
        }
    }

    fun nodejs() = nodejs { }
    fun nodejs(body: KotlinJsNodeDsl.() -> Unit)
    fun nodejs(fn: Action<KotlinJsNodeDsl>) {
        nodejs {
            fn.execute(this)
        }
    }

    fun useCommonJs()
    fun useEsModules()

    fun generateTypeScriptDefinitions()

    val binaries: KotlinJsBinaryContainer

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
    override val compilations: NamedDomainObjectContainer<out KotlinJsCompilation>
}

interface KotlinJsSubTargetDsl {
    @Deprecated("Please use distribution(Action)")
    @ExperimentalDistributionDsl
    fun distribution(body: Distribution.() -> Unit) {
        distribution(Action {
            it.body()
        })
    }

    @ExperimentalDistributionDsl
    fun distribution(body: Action<Distribution>)

    @Deprecated("Please use testTask(Action)")
    fun testTask(body: KotlinJsTest.() -> Unit) {
        testTask(Action {
            it.body()
        })
    }

    fun testTask(body: Action<KotlinJsTest>)

    val testRuns: NamedDomainObjectContainer<KotlinJsPlatformTestRun>
}

interface KotlinJsBrowserDsl : KotlinJsSubTargetDsl {
    @Deprecated("Please use commonWebpackConfig(Action)")
    fun commonWebpackConfig(body: KotlinWebpackConfig.() -> Unit) {
        commonWebpackConfig(Action {
            it.body()
        })
    }

    fun commonWebpackConfig(body: Action<KotlinWebpackConfig>)

    @Deprecated("Please use runTask(Action)")
    fun runTask(body: KotlinWebpack.() -> Unit) {
        runTask(Action {
            it.body()
        })
    }

    fun runTask(body: Action<KotlinWebpack>)

    @Deprecated("Please use webpackTask(Action)")
    fun webpackTask(body: KotlinWebpack.() -> Unit) {
        webpackTask(Action {
            it.body()
        })
    }

    fun webpackTask(body: Action<KotlinWebpack>)

    @Deprecated("Please use dceTask(Action)")
    @ExperimentalDceDsl
    fun dceTask(body: KotlinJsDce.() -> Unit) {
        dceTask(Action {
            it.body()
        })
    }

    @ExperimentalDceDsl
    fun dceTask(body: Action<KotlinJsDce>)
}

interface KotlinJsNodeDsl : KotlinJsSubTargetDsl {
    @Deprecated("Please use runTask(Action)")
    fun runTask(body: NodeJsExec.() -> Unit) {
        runTask(Action {
            it.body()
        })
    }

    fun runTask(body: Action<NodeJsExec>)
}