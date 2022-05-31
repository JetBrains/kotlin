/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import java.io.Serializable

sealed interface IdeaKpmFragmentAnalysisFlag : Serializable {
    val flagName: String
    val flagState: String
}

@InternalKotlinGradlePluginApi
data class IdeaKpmFragmentAnalysisFlagImpl(
    override val flagName: String,
    override val flagState: String
) : IdeaKpmFragmentAnalysisFlag {

    @InternalKotlinGradlePluginApi
    companion object {
        const val serialVersionUID = 0L
    }
}

sealed interface IdeaKpmFragmentAnalysisFlags : Serializable {
    val analysisFlags: Collection<IdeaKpmFragmentAnalysisFlag>
}

@InternalKotlinGradlePluginApi
data class IdeaKpmFragmentAnalysisFlagsImpl(
    override val analysisFlags: Collection<IdeaKpmFragmentAnalysisFlag>
) : IdeaKpmFragmentAnalysisFlags {

    @InternalKotlinGradlePluginApi
    companion object {
        const val serialVersionUID = 0L
    }
}