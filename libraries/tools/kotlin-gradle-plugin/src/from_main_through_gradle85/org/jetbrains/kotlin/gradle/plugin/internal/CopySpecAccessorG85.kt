/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.file.CopySpec

internal class CopySpecAccessorG85(private val copySpec: CopySpec) : CopySpecAccessor {
    override fun filePermission(permission: String) {
        copySpec.fileMode = permission.asBinaryInt
    }

    private val String.asBinaryInt: Int
        get() {
            // Ensure the input is a valid Unix permission string
            check(length == 9 && this.matches(Regex("[rwx-]{9}"))) {
                throw IllegalArgumentException("Invalid Unix permission string. Expected format: rwxrwxrwx. Actual: $this")
            }

            // Map characters to binary digits and join them into a single binary string
            val binaryString = map { char ->
                when (char) {
                    'r', 'w', 'x' -> '1'
                    '-' -> '0'
                    else -> throw IllegalArgumentException("Unexpected character in permission string: $char")
                }
            }.joinToString("")

            return binaryString.toInt(2)
        }

    internal class Factory : CopySpecAccessor.Factory {
        override fun getInstance(copySpec: CopySpec): CopySpecAccessor {
            return CopySpecAccessorG85(copySpec)
        }
    }
}