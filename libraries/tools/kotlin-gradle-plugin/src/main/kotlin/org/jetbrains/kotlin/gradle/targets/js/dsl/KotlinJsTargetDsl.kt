/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dsl

import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

interface KotlinJsTargetDsl {
    fun browser()
    fun browser(body: KotlinJsBrowserDsl.() -> Unit)

    fun nodejs()
    fun nodejs(body: KotlinJsNodeDsl.() -> Unit)
}

interface KotlinJsInnerTargetDsl {
    fun testTask(body: KotlinJsTest.() -> Unit)
}

interface KotlinJsBrowserDsl : KotlinJsInnerTargetDsl {
    fun runTask(body: KotlinWebpack.() -> Unit)
}

interface KotlinJsNodeDsl : KotlinJsInnerTargetDsl {
    fun runTask(body: NodeJsExec.() -> Unit)
}