/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

object IdeBuiltInsLoadingState {
    val state: IdeBuiltInsLoading = IdeBuiltInsLoading.FROM_DEPENDENCIES_JVM

    enum class IdeBuiltInsLoading {
        FROM_CLASSLOADER,
        FROM_DEPENDENCIES_JVM;
    }

    val isFromDependenciesForJvm: Boolean
        get() = state == IdeBuiltInsLoading.FROM_DEPENDENCIES_JVM

    val isFromClassLoader: Boolean
        get() = state == IdeBuiltInsLoading.FROM_CLASSLOADER
}
