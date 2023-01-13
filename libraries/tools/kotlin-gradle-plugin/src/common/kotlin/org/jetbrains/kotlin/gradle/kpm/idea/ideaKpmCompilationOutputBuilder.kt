/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmCompilationOutput
import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmCompilationOutputImpl
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationOutput

internal fun IdeaKpmCompilationOutput(compilation: KotlinCompilationOutput): IdeaKpmCompilationOutput {
    return IdeaKpmCompilationOutputImpl(
        classesDirs = compilation.classesDirs.toSet(),
        resourcesDir = compilation.resourcesDir
    )
}
