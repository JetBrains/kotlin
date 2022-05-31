/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import java.io.Serializable

sealed interface IdeaKpmFragmentLanguageFeature : Serializable {
    val featureName: String
    val featureState: String
}

@InternalKotlinGradlePluginApi
data class IdeaKpmFragmentLanguageFeatureImpl(
    override val featureName: String,
    override val featureState: String
) : IdeaKpmFragmentLanguageFeature {

    @InternalKotlinGradlePluginApi
    companion object {
        const val serialVersionUID = 0L
    }
}

sealed interface IdeaKpmFragmentLanguageFeatures : Serializable {
    val languageFeatures: Collection<IdeaKpmFragmentLanguageFeature>
}

@InternalKotlinGradlePluginApi
data class IdeaKpmFragmentLanguageFeaturesImpl(
    override val languageFeatures: Collection<IdeaKpmFragmentLanguageFeature>
) : IdeaKpmFragmentLanguageFeatures {

    @InternalKotlinGradlePluginApi
    companion object {
        const val serialVersionUID = 0L
    }
}