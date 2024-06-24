/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation.api.klib

import java.io.Serializable

public class KlibSignatureVersion internal constructor(internal val version: Int) : Serializable {

    public companion object {
        public fun of(value: Int): KlibSignatureVersion {
            require(value >= 1) {
                "Invalid version value, expected positive value: $value"
            }
            return KlibSignatureVersion(value)
        }

        public val LATEST: KlibSignatureVersion = KlibSignatureVersion(Int.MIN_VALUE)

        @JvmStatic
        private val serialVersionUID: Long = 1
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KlibSignatureVersion) return false

        return version == other.version
    }

    override fun hashCode(): Int {
        return version.hashCode()
    }

    override fun toString(): String {
        val versionStr = when(version) {
            LATEST.version -> "LATEST"
            else -> version.toString()
        }
        return "KlibSignatureVersion($versionStr)"
    }
}
