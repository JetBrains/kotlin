/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationModuleManager
import org.jetbrains.kotlin.gradle.plugin.mpp.filterModuleName
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName

internal object DefaultCompilationModuleFactory : KotlinCompilationImplFactory.CompilationModuleFactory {
    override fun create(target: KotlinTarget, compilationName: String): KotlinCompilationModuleManager.CompilationModule =
        KotlinCompilationModuleManager.CompilationModule(
            compilationName = compilationName,
            ownModuleName = target.project.provider {
                val baseName = target.project.archivesName.orNull
                    ?: target.project.name
                val suffix = if (compilationName == KotlinCompilation.MAIN_COMPILATION_NAME) "" else "_$compilationName"
                filterModuleName("$baseName$suffix")
            },
            type = if (compilationName == KotlinCompilation.MAIN_COMPILATION_NAME) KotlinCompilationModuleManager.CompilationModule.Type.Main
            else KotlinCompilationModuleManager.CompilationModule.Type.Auxiliary
        )
}
