/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationOutput
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultKotlinCompilationOutput

internal object DefaultKotlinCompilationOutputFactory : KotlinCompilationImplFactory.KotlinCompilationOutputFactory {
    override fun create(target: KotlinTarget, compilationName: String): KotlinCompilationOutput = DefaultKotlinCompilationOutput(
        target.project,
        target.project.layout.buildDirectory.dir("processedResources/${target.targetName}/$compilationName")
    )
}
