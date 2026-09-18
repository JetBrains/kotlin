/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.archive

import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.HasProject
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

internal interface KotlinTargetWithKotlinArchiveSupport : HasProject {
    val isStoredInKotlinArchive: Provider<Boolean>
    val platformNameInKotlinArchive: String
}

internal val KotlinTarget.isStoredInKotlinArchive: Provider<Boolean>
    get() = if (this is KotlinTargetWithKotlinArchiveSupport)
        isStoredInKotlinArchive
    else
        project.provider { false }
