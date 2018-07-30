/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.noarg.gradle.model.impl

import org.jetbrains.kotlin.gradle.model.NoArg
import java.io.Serializable

/**
 * Implementation of the [NoArg] interface.
 */
data class NoArgImpl(
    override val name: String,
    override val annotations: List<String>,
    override val presets: List<String>,
    override val isInvokeInitializers: Boolean
) : NoArg, Serializable {

    override val modelVersion = serialVersionUID

    companion object {
        private const val serialVersionUID = 1L
    }
}