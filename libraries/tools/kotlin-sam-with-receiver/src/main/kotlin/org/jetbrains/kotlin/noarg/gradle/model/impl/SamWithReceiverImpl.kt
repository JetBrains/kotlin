/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.noarg.gradle.model.impl

import org.jetbrains.kotlin.gradle.model.SamWithReceiver
import java.io.Serializable

/**
 * Implementation of the [SamWithReceiver] interface.
 */
data class SamWithReceiverImpl(
    private val myName: String,
    private val myAnnotations: List<String>,
    private val myPresets: List<String>
) : SamWithReceiver, Serializable {

    override fun getModelVersion(): Long {
        return serialVersionUID
    }

    override fun getName(): String {
        return myName
    }

    override fun getAnnotations(): List<String> {
        return myAnnotations
    }

    override fun getPresets(): List<String> {
        return myPresets
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}