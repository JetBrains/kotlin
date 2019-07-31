/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dsl

import groovy.lang.Closure
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

interface KotlinJsTargetDsl {
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
}

interface KotlinJsSubTargetDsl {
    fun testTask(body: KotlinJsTest.() -> Unit)
    fun testTask(fn: Closure<*>) {
        testTask {
            ConfigureUtil.configure(fn, this)
        }
    }
}

interface KotlinJsBrowserDsl : KotlinJsSubTargetDsl {
    fun runTask(body: KotlinWebpack.() -> Unit)
    fun runTask(fn: Closure<*>) {
        runTask {
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