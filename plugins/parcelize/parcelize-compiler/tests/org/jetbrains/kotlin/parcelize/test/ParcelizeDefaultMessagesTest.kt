/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.test

import org.jetbrains.kotlin.parcelize.fir.diagnostics.KtDefaultErrorMessagesParcelize
import org.jetbrains.kotlin.parcelize.fir.diagnostics.KtErrorsParcelize
import org.jetbrains.kotlin.test.utils.verifyMessages
import org.junit.jupiter.api.Test

class ParcelizeDefaultMessagesTest {
    @Test
    fun ensureAllMessagesPresent() {
        KtDefaultErrorMessagesParcelize.MAP.verifyMessages(KtErrorsParcelize)
    }
}
