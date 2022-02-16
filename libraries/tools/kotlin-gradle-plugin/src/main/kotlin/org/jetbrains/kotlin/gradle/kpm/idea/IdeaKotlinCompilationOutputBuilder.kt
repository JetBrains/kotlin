/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationOutput

internal fun KotlinCompilationOutput.toIdeaKotlinCompilationOutputs(): IdeaKotlinCompilationOutput {
    return IdeaKotlinCompilationOutputImpl(
        classesDirs = this.classesDirs.toSet(),
        resourcesDir = this.resourcesDir
    )
}
