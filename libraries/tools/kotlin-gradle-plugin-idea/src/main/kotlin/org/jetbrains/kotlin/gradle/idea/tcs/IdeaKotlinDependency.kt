/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.tcs

import org.jetbrains.kotlin.tooling.core.HasMutableExtras
import java.io.Serializable

@IdeaKotlinModel
sealed interface IdeaKotlinDependency : Serializable, HasMutableExtras {
    val coordinates: IdeaKotlinDependencyCoordinates?
}
