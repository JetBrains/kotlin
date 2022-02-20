/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.project.model.KotlinModuleIdentifier
import org.jetbrains.kotlin.project.model.LocalModuleIdentifier
import org.jetbrains.kotlin.project.model.MavenModuleIdentifier

internal fun KotlinModuleIdentifier.toIdeaKotlinModuleIdentifier(): IdeaKotlinModuleIdentifier {
    return when (this) {
        is LocalModuleIdentifier -> IdeaKotlinLocalModuleIdentifierImpl(
            moduleClassifier, buildId = buildId, projectId = projectId
        )
        is MavenModuleIdentifier -> IdeaKotlinMavenModuleIdentifierImpl(
            moduleClassifier, group = group, name = name
        )
    }
}
