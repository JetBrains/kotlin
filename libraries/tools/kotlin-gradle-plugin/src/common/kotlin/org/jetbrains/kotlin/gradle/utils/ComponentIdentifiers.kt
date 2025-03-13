/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

@Deprecated("CompositeProjectComponentArtifactMetadata is an internal Gradle class. References to it must be removed for compatibility reasons. Scheduled for removal in Kotlin 2.4.")
@Suppress("unused")
val CompositeProjectComponentArtifactMetadata: Class<*> =
    Class.forName("org.gradle.composite.internal.CompositeProjectComponentArtifactMetadata")

// is operator for classes which cannot be imported
@Deprecated(
    "This function is an internal Kotlin Gradle Plugin utility that is no longer used. Scheduled for removal in Kotlin 2.4.",
    ReplaceWith("clazz.isAssignableFrom(this::class.java)"),
)
@Suppress("unused")
infix fun Any.`is`(clazz: Class<*>): Boolean {
    return clazz.isAssignableFrom(this::class.java)
}
