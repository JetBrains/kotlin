/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven.test

import java.io.File
import java.util.Properties
import kotlin.io.inputStream

data class MavenInvokerProperties(
    val goals: List<String>?,
    val buildResult: String?,
) {
    val failureExpected: Boolean get() = buildResult?.lowercase() == "failure"
}

fun loadMavenInvokerPropertiesOrNull(file: File): MavenInvokerProperties? {
    if (!file.exists()) return null

    val props = Properties()
    props.load(file.inputStream())

    return MavenInvokerProperties(
        goals = props.getProperty("invoker.goals")?.split(" "),
        buildResult = props.getProperty("invoker.buildResult"),
    )
}