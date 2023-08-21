/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.gradle.model.impl

import org.jetbrains.kotlin.gradle.model.FormVer
import java.io.Serializable

/**
 * Implementation of the [FormVer] interface.
 */
data class FormVerImpl(
    override val name: String,
    override val logLevel: String?,
    override val unsupportedFeatureBehaviour: String?,
) : FormVer, Serializable {
    override val modelVersion = serialVersionUID

    companion object {
        private const val serialVersionUID = 1L
    }
}