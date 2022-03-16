/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import java.io.Serializable

interface IdeaKotlinModule : Serializable {
    val coordinates: IdeaKotlinModuleCoordinates
    val fragments: List<IdeaKotlinFragment>
}

val IdeaKotlinModule.name get() = coordinates.moduleName

val IdeaKotlinModule.moduleClassifier get() = coordinates.moduleClassifier

@InternalKotlinGradlePluginApi
data class IdeaKotlinModuleImpl(
    override val coordinates: IdeaKotlinModuleCoordinates,
    override val fragments: List<IdeaKotlinFragment>
) : IdeaKotlinModule {

    @InternalKotlinGradlePluginApi
    companion object {
        private const val serialVersionUID = 0L
    }
}
