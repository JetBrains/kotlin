/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build

import java.security.MessageDigest

/**
 * @return the calculated SHA-256 hash as a string in the hexadecimal format.
 */
internal fun calculateSha256ForString(input: String): String {
    val messageDigest = MessageDigest.getInstance("SHA-256")
    val hash = messageDigest.digest(input.toByteArray(Charsets.UTF_8))
    return hash.joinToString("") { "%02x".format(it) }
}