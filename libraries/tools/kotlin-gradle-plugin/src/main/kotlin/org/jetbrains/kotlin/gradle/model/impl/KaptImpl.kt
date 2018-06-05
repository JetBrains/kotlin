/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model.impl

import org.jetbrains.kotlin.gradle.model.Kapt
import org.jetbrains.kotlin.gradle.model.KaptSourceSet
import java.io.Serializable

/**
 * Implementation of the [Kapt] interface.
 */
data class KaptImpl(
    private val myName: String,
    private val kaptSourceSets: Collection<KaptSourceSet>
) : Kapt, Serializable {

    override fun getModelVersion(): Long {
        return serialVersionUID
    }

    override fun getName(): String {
        return myName
    }

    override fun getKaptSourceSets(): Collection<KaptSourceSet> {
        return kaptSourceSets
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}