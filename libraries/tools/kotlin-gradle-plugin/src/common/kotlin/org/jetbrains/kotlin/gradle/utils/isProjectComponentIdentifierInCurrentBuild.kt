/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult

/**
 * For composite builds gradle replaces [ModuleComponentIdentifier] with [ProjectComponentIdentifier]
 * for which [build.isCurrentBuild] is false
 * Often, these dependencies needs to be handled differently not as usual ProjectDependencies
 * Since included build *has to* produce their artifacts on which "current build projects" will depend on.
 */
internal val ComponentIdentifier.isProjectComponentIdentifierInCurrentBuild: Boolean
    get() = currentBuildProjectIdOrNull != null

internal val ComponentIdentifier.currentBuildProjectIdOrNull
    get(): ProjectComponentIdentifier? = when {
        this is ProjectComponentIdentifier && build.isCurrentBuild -> this
        else -> null
    }

internal val ResolvedComponentResult.currentBuildProjectIdOrNull get() = id.currentBuildProjectIdOrNull
