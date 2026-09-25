/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.gradle.kotlin.dsl

import org.gradle.api.Project
import org.jetbrains.kotlin.build.nodejs.NodeJsExtension

val Project.nodeJsKotlinBuild: NodeJsExtension
    get() = extensions.getByName("nodeJsKotlinBuild") as NodeJsExtension

fun Project.nodeJsKotlinBuild(configure: NodeJsExtension.() -> Unit): Unit =
    nodeJsKotlinBuild.configure()

val Project.wasmNodeJsKotlinBuild: NodeJsExtension
    get() = extensions.getByName("wasmNodeJsKotlinBuild") as NodeJsExtension

fun Project.wasmNodeJsKotlinBuild(configure: NodeJsExtension.() -> Unit): Unit =
    wasmNodeJsKotlinBuild.configure()
