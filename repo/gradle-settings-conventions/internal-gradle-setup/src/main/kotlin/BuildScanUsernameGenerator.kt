/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build

import java.security.MessageDigest

internal class BuildScanUsernameGenerator : SyntheticPropertiesGenerator {
    private fun calculateUsernameHash(salt: String): String {
        val username = System.getProperty("user.name")
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val hash = messageDigest.digest((salt + username).toByteArray(Charsets.UTF_8))
        return hash.fold("") { str, it -> str + "%02x".format(it) }
    }

    override fun generate(setupFile: SetupFile): Map<String, String> {
        val salt = setupFile.obfuscationSalt ?: return emptyMap()
        return mapOf("kotlin.build.scan.username" to calculateUsernameHash(salt))
    }
}