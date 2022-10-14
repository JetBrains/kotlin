/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory

import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationTaskNamesContainer
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

internal object DefaultKotlinCompilationTaskNamesContainerFactory :
    KotlinCompilationImplFactory.KotlinCompilationTaskNamesContainerFactory {
    override fun create(target: KotlinTarget, compilationName: String) = KotlinCompilationTaskNamesContainer(
        compileTaskName = lowerCamelCaseName(
            "compile", compilationName.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME }, "Kotlin", target.targetName
        ),
        compileAllTaskName = lowerCamelCaseName(target.disambiguationClassifier, compilationName, "classes")
    )
}


internal class JvmWithJavaCompilationTaskNamesContainerFactory(private val javaSourceSet: SourceSet) :
    KotlinCompilationImplFactory.KotlinCompilationTaskNamesContainerFactory {
    override fun create(target: KotlinTarget, compilationName: String): KotlinCompilationTaskNamesContainer =
        DefaultKotlinCompilationTaskNamesContainerFactory.create(target, compilationName)
            .copy(compileAllTaskName = javaSourceSet.classesTaskName)
}


internal object NativeKotlinCompilationTaskNamesContainerFactory :
    KotlinCompilationImplFactory.KotlinCompilationTaskNamesContainerFactory {
    override fun create(target: KotlinTarget, compilationName: String) =
        DefaultKotlinCompilationTaskNamesContainerFactory.create(target, compilationName)
            .copy(compileAllTaskName = lowerCamelCaseName(target.disambiguationClassifier, compilationName, "klibrary"))
}

