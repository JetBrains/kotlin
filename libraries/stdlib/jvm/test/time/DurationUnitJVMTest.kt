/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.time

import java.util.concurrent.TimeUnit
import kotlin.test.*
import kotlin.time.*

class DurationUnitJVMTest {
    @Test
    fun conversionFromTimeUnit() {
        for (unit in DurationUnit.entries) {
            val timeUnit = unit.toTimeUnit()
            assertEquals(unit.name, timeUnit.name)
            assertEquals(unit, timeUnit.toDurationUnit())
        }

        for (timeUnit in TimeUnit.entries) {
            val unit = timeUnit.toDurationUnit()
            assertEquals(timeUnit.name, unit.name)
            assertEquals(timeUnit, unit.toTimeUnit())
        }
    }
}
