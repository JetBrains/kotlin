/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import java.io.Serializable

sealed interface IdeaKpmModule : Serializable {
    val coordinates: IdeaKpmModuleCoordinates
    val fragments: List<IdeaKpmFragment>
}

val IdeaKpmModule.name get() = coordinates.moduleName

val IdeaKpmModule.moduleClassifier get() = coordinates.moduleClassifier

@InternalKotlinGradlePluginApi
data class IdeaKpmModuleImpl(
    override val coordinates: IdeaKpmModuleCoordinates,
    override val fragments: List<IdeaKpmFragment>
) : IdeaKpmModule {

    @InternalKotlinGradlePluginApi
    companion object {
        private const val serialVersionUID = 0L
    }
}
