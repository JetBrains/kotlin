/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model

open class KotlinAttributeKey(
    val uniqueName: String
) {
    override fun equals(other: Any?): Boolean =
        other is KotlinAttributeKey && uniqueName == other.uniqueName

    override fun hashCode(): Int =
        uniqueName.hashCode()
}

object KotlinPlatformTypeAttribute : KotlinAttributeKey("org.jetbrains.kotlin.platform.type") {
    const val JVM = "jvm"
    const val JS = "js"
    const val NATIVE = "native"
}

object KotlinNativeTargetAttribute : KotlinAttributeKey("org.jetbrains.kotlin.native.target")