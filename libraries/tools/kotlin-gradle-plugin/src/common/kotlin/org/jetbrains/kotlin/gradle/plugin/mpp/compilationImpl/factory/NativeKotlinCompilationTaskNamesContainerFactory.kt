/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory

import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

internal object NativeKotlinCompilationTaskNamesContainerFactory :
    KotlinCompilationImplFactory.KotlinCompilationTaskNamesContainerFactory {
    override fun create(target: KotlinTarget, compilationName: String) =
        DefaultKotlinCompilationTaskNamesContainerFactory.create(target, compilationName)
            .copy(compileAllTaskName = lowerCamelCaseName(target.disambiguationClassifier, compilationName, "klibrary"))
}
