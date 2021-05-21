/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_COMMON_LIBS_DIR
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR
import java.io.File
import java.security.MessageDigest
import java.util.*


public fun interface CommonizerOutputLayout {
    public fun getTargetDirectory(root: File, target: CommonizerTarget): File
}

public object NativeDistributionCommonizerOutputLayout : CommonizerOutputLayout {
    override fun getTargetDirectory(root: File, target: CommonizerTarget): File {
        return when (target) {
            is LeafCommonizerTarget -> root.resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR).resolve(target.name)
            is SharedCommonizerTarget -> root.resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR)
        }
    }
}

public object HierarchicalCommonizerOutputLayout : CommonizerOutputLayout {
    internal const val maxFileNameLength = 150

    override fun getTargetDirectory(root: File, target: CommonizerTarget): File {
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

    private val CommonizerTarget.identityStringHash: String
        get() {
            val sha = MessageDigest.getInstance("SHA-1")
            val base64 = Base64.getUrlEncoder()
            return base64.encode(sha.digest(identityString.encodeToByteArray())).decodeToString()
        }
}

