/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer

/**
 * KGP's internal analog of [org.gradle.api.internal.attributes.AttributeContainerInternal.asMap]
 * Can be used to compare attributes
 */
internal fun AttributeContainer.toMap(): Map<Attribute<*>, Any?> {
    val result = mutableMapOf<Attribute<*>, Any?>()
    for (key in keySet()) {
        result[key] = getAttribute(key)
    }

    return result
}