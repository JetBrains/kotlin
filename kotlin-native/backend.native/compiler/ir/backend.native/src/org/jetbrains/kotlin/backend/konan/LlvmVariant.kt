/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.HostManager

sealed class LlvmVariant {
    object User : LlvmVariant() {
        override fun getKonanPropertiesEntry(): Pair<String, String> =
                konanPropertiesKey to "\$llvm.${HostManager.hostName}.user"
    }

    object Dev : LlvmVariant() {
        override fun getKonanPropertiesEntry(): Pair<String, String> =
                konanPropertiesKey to "\$llvm.${HostManager.hostName}.user"

    }

    class Custom(val path: File) : LlvmVariant() {
        override fun getKonanPropertiesEntry(): Pair<String, String> =
                konanPropertiesKey to path.canonicalPath

    }

    abstract fun getKonanPropertiesEntry(): Pair<String, String>

    companion object {
        private val konanPropertiesKey: String by lazy {
            "llvmHome.${HostManager.hostName}"
        }
    }
}
