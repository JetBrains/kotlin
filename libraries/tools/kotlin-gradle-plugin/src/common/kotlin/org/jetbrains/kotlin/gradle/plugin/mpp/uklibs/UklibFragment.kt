/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.File

internal data class UklibFragment(
    @get:Input
    val identifier: String,
    @get:Input
    val attributes: Set<String>,
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    val files: List<File>,
) {
    constructor(
        identifier: String,
        attributes: Set<String>,
        file: File,
    ) : this(
        identifier,
        attributes,
        listOf(file),
    )

    @get:Internal
    val singleExpectedFileFromModularUklib: File get() = files.single()
}
