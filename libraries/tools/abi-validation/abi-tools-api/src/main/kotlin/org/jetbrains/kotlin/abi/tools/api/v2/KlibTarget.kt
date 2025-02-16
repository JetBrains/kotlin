/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.api.v2

import java.io.Serializable


/**
 * Target name consisting of two parts: a [configurableName] that could be configured by a user, and a [targetName]
 * that names a target platform and could not be configured by a user.
 *
 * When serialized, the target is represented as a tuple `<targetName>.<configurableName>`, like `iosArm64.ios`.
 * If both names are the same (they are by default, unless a user decides to use a custom name), the serialized
 * from is shortened to a single term. For example, `macosArm64.macosArm64` and `macosArm64` are a long and a short
 * serialized forms of the same target.
 */
public class KlibTarget(
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

        /**
         * Get KLib target by Konan target name.
         */
        public fun fromKonanTargetName(konanName: String): KlibTarget {
            val targetName = konanTargetNameMapping[konanName] ?: throw IllegalArgumentException("Konan name '$konanName' not found")
            return KlibTarget(targetName, targetName)
        }

        /**
         * Get Konan names for supported targets.
         */
        public fun supportedKonanNames(): Set<String> {
            return konanTargetNameMapping.keys
        }
    }

    /**
     * Create copy of this target with new configurable name from [newConfigurableName] parameter.
     */
    public fun configureName(newConfigurableName: String): KlibTarget {
        return KlibTarget(targetName, newConfigurableName)
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

public fun KlibTarget(name: String): KlibTarget = KlibTarget(name, name)

internal val konanTargetNameMapping = mapOf(
    "android_x64" to "androidNativeX64",
    "android_x86" to "androidNativeX86",
    "android_arm32" to "androidNativeArm32",
    "android_arm64" to "androidNativeArm64",
    "ios_arm64" to "iosArm64",
    "ios_x64" to "iosX64",
    "ios_simulator_arm64" to "iosSimulatorArm64",
    "watchos_arm32" to "watchosArm32",
    "watchos_arm64" to "watchosArm64",
    "watchos_x64" to "watchosX64",
    "watchos_simulator_arm64" to "watchosSimulatorArm64",
    "watchos_device_arm64" to "watchosDeviceArm64",
    "tvos_arm64" to "tvosArm64",
    "tvos_x64" to "tvosX64",
    "tvos_simulator_arm64" to "tvosSimulatorArm64",
    "linux_x64" to "linuxX64",
    "mingw_x64" to "mingwX64",
    "macos_x64" to "macosX64",
    "macos_arm64" to "macosArm64",
    "linux_arm64" to "linuxArm64",
    "ios_arm32" to "iosArm32",
    "watchos_x86" to "watchosX86",
    "linux_arm32_hfp" to "linuxArm32Hfp",
    "mingw_x86" to "mingwX86",
    "wasm-wasi" to "wasmWasi",
    "wasm-js" to "wasmJs"
)
