/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import java.io.File
import java.security.MessageDigest
import java.util.*

public object CommonizerOutputFileLayout {
    internal const val maxFileNameLength = 150

    public fun getCommonizedDirectory(root: File, target: CommonizerTarget): File {
        return root.resolve(target.fileName)
    }

    public val CommonizerTarget.fileName: String
        get() {
            val identityString = identityString
            return if (identityString.length <= maxFileNameLength) identityString
            else {
                val hashSuffix = "[--$identityStringHash]"
                return identityString.take(maxFileNameLength - hashSuffix.length) + hashSuffix
            }
        }

    public val Set<CommonizerTarget>.fileName: String
        get() = this.joinToString(";") { it.identityString }.base64Hash


    private val CommonizerTarget.identityStringHash: String
        get() = identityString.base64Hash

    private val String.base64Hash: String
        get() {
            val sha = MessageDigest.getInstance("SHA-1")
            val base64 = Base64.getUrlEncoder()
            return base64.encode(sha.digest(this.encodeToByteArray())).decodeToString()
        }
}

