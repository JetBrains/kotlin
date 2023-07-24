/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import clib.myCFunction

object ConsumerALinuxX64 {
    init {
        ProducerACommonX
        ProducerANative
        ProducerALinuxX64

        @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
        myCFunction()
    }
}