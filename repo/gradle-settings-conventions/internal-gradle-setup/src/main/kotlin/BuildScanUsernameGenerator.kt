/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build

internal class BuildScanUsernameGenerator : SyntheticPropertiesGenerator {
    private fun calculateUsernameHash(salt: String): String {
        val username = System.getProperty("user.name")
        return calculateSha256ForString(salt + username)
    }

    override fun generate(setupFile: SetupFile): Map<String, String> {
        val salt = setupFile.obfuscationSalt ?: return emptyMap()
        return mapOf("kotlin.build.scan.username" to calculateUsernameHash(salt))
    }
}