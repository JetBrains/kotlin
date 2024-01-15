/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import java.io.File

/**
 * @suppress TODO: KT-58858 add documentation
 */
interface KotlinCompilationOutput {
    var resourcesDirProvider: Any
    val resourcesDir: File
    val classesDirs: ConfigurableFileCollection
    val allOutputs: FileCollection
}