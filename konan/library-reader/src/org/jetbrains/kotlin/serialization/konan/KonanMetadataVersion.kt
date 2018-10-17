/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.konan

import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion

class KonanMetadataVersion(vararg numbers: Int) : BinaryVersion(*numbers) {

    override fun isCompatible(): Boolean = this.major == 1 && this.minor == 0

    companion object {
        @JvmField
        val INSTANCE = KonanMetadataVersion(1, 0, 0)

        @JvmField
        val INVALID_VERSION = KonanMetadataVersion()
    }
}
