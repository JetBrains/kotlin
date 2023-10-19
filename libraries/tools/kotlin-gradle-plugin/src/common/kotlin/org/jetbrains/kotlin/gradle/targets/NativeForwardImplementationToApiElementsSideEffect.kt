/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

/**
 * Kotlin/Multiplatform requires all dependencies to be available at compile time.
 * We therefore forward all dependencies with the 'implementation' scope to 'apiElements' to ensure
 * them being available transitively.
 */
internal val NativeForwardImplementationToApiElementsSideEffect = KotlinTargetSideEffect<KotlinNativeTarget> { target ->
    val configurations = target.project.configurations

    // The configuration and the main compilation are created by the base class.
    val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
    configurations.getByName(target.apiElementsConfigurationName).apply {
        //  K/N and K/JS IR compiler doesn't divide libraries into implementation and api ones. So we need to add implementation
        // dependencies into the outgoing configuration.
        extendsFrom(configurations.getByName(mainCompilation.implementationConfigurationName))
    }
}