/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.prepare
import java.io.File

@OptIn(ExperimentalStdlibApi::class)
fun main(args: Array<String>) {
    val copyProjectsRoot = File(args[0])
    KpmDependencyResolutionIT.testCases.forEach { testCase ->
        val prepared = object : BaseGradleIT() { }.prepare(testCase)
        val projectDir = prepared.project.projectDir
        projectDir.copyRecursively(copyProjectsRoot.resolve(testCase.name.orEmpty().replace(" ", "_")))
    }
}
