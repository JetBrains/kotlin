/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dsl

import groovy.lang.Closure
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.dsl.KotlinJsDce
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsPlatformTestRun
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsReportAggregatingTestRun
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

interface KotlinJsSubtargetContainerDsl : KotlinTarget {
    val nodejs: KotlinJsNodeDsl

    val browser: KotlinJsBrowserDsl

    val isNodejsConfigured: Boolean

    val isBrowserConfigured: Boolean

    fun whenNodejsConfigured(body: KotlinJsNodeDsl.() -> Unit)

    fun whenBrowserConfigured(body: KotlinJsBrowserDsl.() -> Unit)
}

interface KotlinJsTargetDsl : KotlinTarget {
    fun browser() = browser { }
    fun browser(body: KotlinJsBrowserDsl.() -> Unit)
    fun browser(fn: Closure<*>) {
        browser {
            ConfigureUtil.configure(fn, this)
        }
    }

    fun nodejs() = nodejs { }
    fun nodejs(body: KotlinJsNodeDsl.() -> Unit)
    fun nodejs(fn: Closure<*>) {
        nodejs {
            ConfigureUtil.configure(fn, this)
        }
    }

    fun produceKotlinLibrary()

    fun produceExecutable()

    val testRuns: NamedDomainObjectContainer<KotlinJsReportAggregatingTestRun>
}

interface KotlinJsSubTargetDsl {
    fun testTask(body: KotlinJsTest.() -> Unit)
    fun testTask(fn: Closure<*>) {
        testTask {
            ConfigureUtil.configure(fn, this)
        }
    }

    val testRuns: NamedDomainObjectContainer<KotlinJsPlatformTestRun>
}

interface KotlinJsIrSubTargetDsl {
    fun testTask(body: KotlinJsTest.() -> Unit)
    fun testTask(fn: Closure<*>) {
        testTask {
            ConfigureUtil.configure(fn, this)
        }
    }

    val testRuns: NamedDomainObjectContainer<KotlinJsPlatformTestRun>
}

interface KotlinJsBrowserDsl : KotlinJsSubTargetDsl {
    fun runTask(body: KotlinWebpack.() -> Unit)
    fun runTask(fn: Closure<*>) {
        runTask {
            ConfigureUtil.configure(fn, this)
        }
    }

    @ExperimentalDistributionDsl
    fun distribution(body: Distribution.() -> Unit)

    @ExperimentalDistributionDsl
    fun distribution(fn: Closure<*>) {
        distribution {
            ConfigureUtil.configure(fn, this)
        }
    }

    fun webpackTask(body: KotlinWebpack.() -> Unit)
    fun webpackTask(fn: Closure<*>) {
        webpackTask {
            ConfigureUtil.configure(fn, this)
        }
    }

    @ExperimentalDceDsl
    fun dceTask(body: KotlinJsDce.() -> Unit)

    @ExperimentalDceDsl
    fun dceTask(fn: Closure<*>) {
        dceTask {
            ConfigureUtil.configure(fn, this)
        }
    }
}

interface KotlinJsIrBrowserDsl : KotlinJsIrSubTargetDsl {
    fun runTask(body: KotlinWebpack.() -> Unit)
    fun runTask(fn: Closure<*>) {
        runTask {
            ConfigureUtil.configure(fn, this)
        }
    }

    @ExperimentalDistributionDsl
    fun distribution(body: Distribution.() -> Unit)

    @ExperimentalDistributionDsl
    fun distribution(fn: Closure<*>) {
        distribution {
            ConfigureUtil.configure(fn, this)
        }
    }

    fun webpackTask(body: KotlinWebpack.() -> Unit)
    fun webpackTask(fn: Closure<*>) {
        webpackTask {
            ConfigureUtil.configure(fn, this)
        }
    }
}

interface KotlinJsNodeDsl : KotlinJsSubTargetDsl {
    fun runTask(body: NodeJsExec.() -> Unit)
}

interface KotlinJsIrNodeDsl : KotlinJsIrSubTargetDsl {
    fun runTask(body: NodeJsExec.() -> Unit)
}