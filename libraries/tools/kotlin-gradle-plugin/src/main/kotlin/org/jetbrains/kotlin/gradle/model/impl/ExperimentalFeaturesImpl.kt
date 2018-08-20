/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model.impl

import org.jetbrains.kotlin.gradle.model.ExperimentalFeatures
import java.io.Serializable

/**
 * Implementation of the [ExperimentalFeatures] interface.
 */
data class ExperimentalFeaturesImpl(
    override val coroutines: String?
) : ExperimentalFeatures, Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }
}