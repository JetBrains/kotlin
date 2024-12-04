/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.gradle.model.impl

import org.jetbrains.kotlin.gradle.model.Lombok
import java.io.File
import java.io.Serializable

data class LombokImpl(override val name: String, override val configurationFile: File?) : Lombok, Serializable {

    override val modelVersion: Long
        get() = serialVersionUID

    companion object {
        private const val serialVersionUID = 1L
    }
}
