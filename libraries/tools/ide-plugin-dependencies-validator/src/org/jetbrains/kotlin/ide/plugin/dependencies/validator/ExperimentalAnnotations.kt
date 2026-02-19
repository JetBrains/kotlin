/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.plugin.dependencies.validator

import java.nio.file.Paths
import kotlin.io.path.readLines

object ExperimentalAnnotations {
    val experimentalAnnotations: Set<String> by lazy {
        Paths.get(EXPERIMENTAL_ANNOTATIONS_PATH)
            .readLines()
            .map { it.trim() }
            .filterNot { it.startsWith("#") || it.isBlank() }
            .toSet()
    }


    val experimentalAnnotationShortNames: Set<String> by lazy {
        experimentalAnnotations.mapTo(mutableSetOf()) { it.substringAfterLast('.') }
    }

    private const val EXPERIMENTAL_ANNOTATIONS_PATH =
        "libraries/tools/ide-plugin-dependencies-validator/ExperimentalAnnotations.txt"
}