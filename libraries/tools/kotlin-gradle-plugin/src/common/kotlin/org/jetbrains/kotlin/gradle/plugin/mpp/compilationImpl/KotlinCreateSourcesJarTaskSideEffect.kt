/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl

import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.plugin.mpp.sourcesJarTask
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

internal val KotlinCreateSourcesJarTaskSideEffect = KotlinCompilationSideEffect { compilation ->
    if(compilation.isMain()) {
        sourcesJarTask(compilation, compilation.target.targetName, compilation.target.targetName.toLowerCaseAsciiOnly())
    }
}