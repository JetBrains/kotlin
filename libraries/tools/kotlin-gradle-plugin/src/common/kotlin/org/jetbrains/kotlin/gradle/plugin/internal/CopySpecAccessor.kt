/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.jetbrains.kotlin.gradle.plugin.VariantImplementationFactories
import org.jetbrains.kotlin.gradle.plugin.variantImplementationFactory

/**
 * Provides [CopySpec] compatibility layer.
 */
internal interface CopySpecAccessor {

    /**
     * Configures [CopySpec] file access permissions.
     *
     * @param permission permissions in unix style (for example, "rwxrwxrwx")
     */
    fun filePermission(permission: String)

    interface Factory : VariantImplementationFactories.VariantImplementationFactory {
        fun getInstance(copySpec: CopySpec): CopySpecAccessor
    }
}

internal class DefaultCopySpecAccessor(private val copySpec: CopySpec) : CopySpecAccessor {
    override fun filePermission(permission: String) {
        copySpec.filePermissions {
            it.unix(permission)
        }
    }

    internal class Factory : CopySpecAccessor.Factory {
        override fun getInstance(copySpec: CopySpec): CopySpecAccessor {
            return DefaultCopySpecAccessor(copySpec)
        }
    }
}

internal fun CopySpec.compatAccessor(project: Project): CopySpecAccessor = project
    .variantImplementationFactory<CopySpecAccessor.Factory>()
    .getInstance(this)
