/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.noarg

object NoArgPluginNames {
    val SUPPORTED_PRESETS = mapOf(
        "jpa" to listOf("javax.persistence.Entity", "javax.persistence.Embeddable", "javax.persistence.MappedSuperclass",
                        "jakarta.persistence.Entity", "jakarta.persistence.Embeddable", "jakarta.persistence.MappedSuperclass")
    )

    const val PLUGIN_ID = "org.jetbrains.kotlin.noarg"
    const val ANNOTATION_OPTION_NAME = "annotation"
    const val INVOKE_INITIALIZERS_OPTION_NAME = "invokeInitializers"
}
