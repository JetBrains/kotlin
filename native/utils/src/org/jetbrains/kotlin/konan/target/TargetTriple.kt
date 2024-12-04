/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

/**
 * Classical way to describe a target platform.
 * Despite it's name, may contain more or less than 3 components.
 *
 * Example: arm64-apple-ios-simulator.
 * - arm64 - target hardware.
 * - apple - vendor.
 * - ios - operating system.
 * - simulator - environment.
 *
 * @see <a href="https://clang.llvm.org/docs/CrossCompilation.html#target-triple">Clang documentation for target triple</a>.
 */
data class TargetTriple(
        val architecture: String,
        val vendor: String,
        val os: String,
        val environment: String?
) {
    companion object {
        /**
         * Parse <arch>-<vendor>-<os>-<environment?> [tripleString].
         *
         * TODO: Support normalization like LLVM's Triple::normalize.
         */
        fun fromString(tripleString: String): TargetTriple {
            val components = tripleString.split('-')
            // TODO: There might be other cases (e.g. of size 2 or 5),
            //  but let's support only these for now.
            require(components.size == 3 || components.size == 4) {
                "Malformed target triple: $tripleString. Expected format: <arch>-<vendor>-<os>-<environment?>."
            }
            return TargetTriple(
                    architecture = components[0],
                    vendor = components[1],
                    os = components[2],
                    environment = components.getOrNull(3)
            )
        }
    }

    override fun toString(): String {
        val envSuffix = environment?.let { "-$environment" }
                ?: ""
        return "$architecture-$vendor-$os$envSuffix"
    }
}

/**
 * Check that given target is Apple simulator.
 */
val TargetTriple.isSimulator: Boolean
    get() = environment == "simulator"

/**
 * Appends version to OS part of triple.
 *
 * Useful for precise target specification in Clang and Swift.
 */
fun TargetTriple.withOSVersion(osVersion: String): TargetTriple =
        copy(os = "${os}${osVersion}")

/**
 * Triple without vendor (second) component.
 *
 * TODO: Actually, this method should return [TargetTriple],
 *  but this class is not that flexible yet.
 */
fun TargetTriple.withoutVendor(): String {
    val envSuffix = environment?.let { "-$environment" }
            ?: ""
    return "$architecture-$os$envSuffix"
}