/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.io.canonicalPathString
import org.jetbrains.kotlin.konan.target.HostManager
import java.nio.file.Path

sealed class LlvmVariant {
    object User : LlvmVariant() {
        override fun getKonanPropertiesEntry(): Pair<String, String> =
                konanPropertiesKey to "\$llvm.${HostManager.Companion.hostName}.user"
    }

    object Dev : LlvmVariant() {
        override fun getKonanPropertiesEntry(): Pair<String, String> =
                konanPropertiesKey to "\$llvm.${HostManager.Companion.hostName}.dev"

    }

    object DevWithAsserts : LlvmVariant() {
        override fun getKonanPropertiesEntry(): Pair<String, String> =
            konanPropertiesKey to "\$llvm.${HostManager.Companion.hostName}.dev-with-asserts"
    }

    class Custom(val path: Path) : LlvmVariant() {
        override fun getKonanPropertiesEntry(): Pair<String, String> =
                konanPropertiesKey to path.canonicalPathString()

    }

    abstract fun getKonanPropertiesEntry(): Pair<String, String>

    companion object {
        private val konanPropertiesKey: String by lazy {
            "llvmHome.${HostManager.Companion.hostName}"
        }
    }
}
