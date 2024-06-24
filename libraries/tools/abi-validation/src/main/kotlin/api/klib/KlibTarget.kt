/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation.api.klib

import java.io.Serializable


/**
 * Target name consisting of two parts: a [configurableName] that could be configured by a user, and an [targetName]
 * that names a target platform and could not be configured by a user.
 *
 * When serialized, the target represented as a tuple `<targetName>.<canonicalName>`, like `ios.iosArm64`.
 * If both names are the same (they are by default, unless a user decides to use a custom name), the serialized
 * from is shortened to a single term. For example, `macosArm64.macosArm64` and `macosArm64` are a long and a short
 * serialized forms of the same target.
 */
public class KlibTarget internal constructor(
    /**
     * An actual name of a target that remains unaffected by a custom name settings in a build script.
     */
    public val targetName: String,
    /**
     * A name of a target that could be configured by a user in a build script.
     * Usually, it's the same name as [targetName].
     */
    public val configurableName: String
) : Serializable {
    init {
        require(!configurableName.contains(".")) {
            "Configurable name can't contain the '.' character: $configurableName"
        }
        require(!targetName.contains(".")) {
            "Target name can't contain the '.' character: $targetName"
        }
    }
    public companion object {
        /**
         * Parses a [KlibTarget] from a [value] string in a long (`<targetName>.<configurableName>`)
         * or a short (`<targetName>`) format.
         *
         * @throws IllegalArgumentException if [value] does not conform the format.
         */
        public fun parse(value: String): KlibTarget {
            require(value.isNotBlank()) { "Target name could not be blank." }
            if (!value.contains('.')) {
                return KlibTarget(value)
            }
            val parts = value.split('.')
            if (parts.size != 2 || parts.any { it.isBlank() }) {
                throw IllegalArgumentException(
                    "Target has illegal name format: \"$value\", expected: <target name>.<underlying target name>"
                )
            }
            return KlibTarget(parts[0], parts[1])
        }

        @JvmStatic
        private val serialVersionUID: Long = 1
    }


    override fun toString(): String =
        if (configurableName == targetName) configurableName else "$targetName.$configurableName"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KlibTarget) return false

        if (configurableName != other.configurableName) return false
        if (targetName != other.targetName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = configurableName.hashCode()
        result = 31 * result + targetName.hashCode()
        return result
    }
}

internal fun KlibTarget(name: String) = KlibTarget(name, name)
