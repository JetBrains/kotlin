/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

val CompositeProjectComponentArtifactMetadata =
    Class.forName("org.gradle.composite.internal.CompositeProjectComponentArtifactMetadata")

// is operator for classes which cannot be imported
infix fun Any.`is`(clazz: Class<*>): Boolean {
    return clazz.isAssignableFrom(this::class.java)
}