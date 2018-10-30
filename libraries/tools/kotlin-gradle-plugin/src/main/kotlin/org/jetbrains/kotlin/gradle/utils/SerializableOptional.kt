/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import java.io.Serializable

internal class SerializableOptional<T>(val value: T?) : Serializable {
    companion object {
        const val serialVersionUID: Long = 0
    }
}