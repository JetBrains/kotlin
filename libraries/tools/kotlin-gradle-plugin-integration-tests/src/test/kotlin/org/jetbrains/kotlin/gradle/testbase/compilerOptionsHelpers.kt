/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

private val KotlinVersion.deprecationAnnotation: Deprecated?
    get() = KotlinVersion::class.java
        .getField(name)
        .declaredAnnotations
        .firstIsInstanceOrNull<Deprecated>()

private val KotlinVersion.isSupported: Boolean
    get() = deprecationAnnotation?.message != "Unsupported"

val KotlinVersion.Companion.firstSupported: KotlinVersion
    get() = KotlinVersion.entries.first { it.isSupported }

private val KotlinVersion.isDeprecated
    get() = deprecationAnnotation != null

val KotlinVersion.Companion.firstNonDeprecated: KotlinVersion
    get() = KotlinVersion.entries.first { !it.isDeprecated }
