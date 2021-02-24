/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

import org.junit.Assert.assertEquals
import org.junit.Test

class KonanTargetTest {
    @Test
    fun allPredefinedTargetsRegistered() {
        assertEquals(
            "Some of predefined KonanTarget instances are not listed in 'KonanTarget.predefinedTargets'",
            KonanTarget::class.sealedSubclasses.mapNotNull { it.objectInstance }.toSet(),
            KonanTarget.predefinedTargets.values.toSet()
        )
    }
}
