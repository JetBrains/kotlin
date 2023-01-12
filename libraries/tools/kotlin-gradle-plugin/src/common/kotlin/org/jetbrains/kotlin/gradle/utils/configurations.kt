/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.artifacts.Configuration

const val COMPILE_ONLY = "compileOnly"
const val COMPILE = "compile"
const val IMPLEMENTATION = "implementation"
const val API = "api"
const val RUNTIME_ONLY = "runtimeOnly"
const val RUNTIME = "runtime"
internal const val INTRANSITIVE = "intransitive"

internal fun Configuration.markConsumable(): Configuration = apply {
    this.isCanBeConsumed = true
    this.isCanBeResolved = false
}

internal fun Configuration.markResolvable(): Configuration = apply {
    this.isCanBeConsumed = false
    this.isCanBeResolved = true
}
