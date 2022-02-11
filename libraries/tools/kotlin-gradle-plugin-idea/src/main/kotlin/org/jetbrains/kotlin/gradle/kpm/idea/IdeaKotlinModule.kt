/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import java.io.Serializable

interface IdeaKotlinModule : Serializable {
    val moduleIdentifier: IdeaKotlinModuleIdentifier
    val fragments: Collection<IdeaKotlinFragment>
}

data class IdeaKotlinModuleImpl @KotlinGradlePluginApi constructor(
    override val moduleIdentifier: IdeaKotlinModuleIdentifier,
    override val fragments: Collection<IdeaKotlinFragment>
) : IdeaKotlinModule
