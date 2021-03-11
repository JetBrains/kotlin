/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.ucache

import com.intellij.openapi.util.io.FileUtil
import java.io.File

/**
 * Safe key for locating sdk in intellij project jdk table
 *
 * null means default sdk
 */
class SdkId private constructor(val homeDirectory: String?) {
    companion object {
        val default = SdkId(null as String?)

        operator fun invoke(homeDirectory: File?): SdkId {
            if (homeDirectory == null) return default
            val canonicalPath = FileUtil.toSystemIndependentName(homeDirectory.canonicalPath)
            return SdkId(canonicalPath)
        }

        operator fun invoke(homeDirectory: String?): SdkId {
            if (homeDirectory == null) return default
            return invoke(File(homeDirectory))
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SdkId

        if (homeDirectory != other.homeDirectory) return false

        return true
    }

    override fun hashCode(): Int {
        return homeDirectory?.hashCode() ?: 0
    }
}