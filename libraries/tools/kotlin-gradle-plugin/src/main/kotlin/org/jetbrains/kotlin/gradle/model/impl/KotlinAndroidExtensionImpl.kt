/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model.impl

import org.jetbrains.kotlin.gradle.model.KotlinAndroidExtension
import java.io.Serializable

/**
 * Implementation of the [KotlinAndroidExtension] interface.
 */
data class KotlinAndroidExtensionImpl(
    private val myName: String,
    private val myIsExperimental: Boolean,
    private val myDefaultCacheImplementation: String?
) : KotlinAndroidExtension, Serializable {

    override fun getModelVersion(): Long {
        return serialVersionUID
    }

    override fun getName(): String {
        return myName
    }

    override fun isExperimental(): Boolean {
        return myIsExperimental
    }

    override fun getDefaultCacheImplementation(): String? {
        return myDefaultCacheImplementation
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}