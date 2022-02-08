/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.time

import samples.*
import kotlin.test.*

import kotlin.time.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class Durations {

    @Sample
    fun toIsoString() {
        assertPrints(25.nanoseconds.toIsoString(), "PT0.000000025S")
        assertPrints(120.3.milliseconds.toIsoString(), "PT0.120300S")
        assertPrints(30.5.seconds.toIsoString(), "PT30.500S")
        assertPrints(30.5.minutes.toIsoString(), "PT30M30S")
        assertPrints(86420.seconds.toIsoString(), "PT24H0M20S")
        assertPrints(2.days.toIsoString(), "PT48H")
        assertPrints(Duration.ZERO.toIsoString(), "PT0S")
        assertPrints(Duration.INFINITE.toIsoString(), "PT9999999999999H")
    }

    @Sample
    fun toStringDefault() {
        assertPrints(45.days, "45d")
        assertPrints(1.5.days, "1d 12h")
        assertPrints(1230.minutes, "20h 30m")
        assertPrints(920.minutes, "15h 20m")
        assertPrints(1.546.seconds, "1.546s")
        assertPrints(25.12.milliseconds, "25.12ms")
    }

    @Sample
    fun toStringDecimals() {
        assertPrints(1230.minutes.toString(DurationUnit.DAYS, 2), "0.85d")
        assertPrints(1230.minutes.toString(DurationUnit.HOURS, 2), "20.50h")
        assertPrints(1230.minutes.toString(DurationUnit.MINUTES), "1230m")
        assertPrints(1230.minutes.toString(DurationUnit.SECONDS), "73800s")
    }

    @Sample
    fun parse() {
        val isoFormatString = "PT1H30M"
        val defaultFormatString = "1h 30m"
        val singleUnitFormatString = "1.5h"
        val invalidFormatString = "1 hour 30 minutes"

        assertPrints(Duration.parse(isoFormatString), "1h 30m")
        assertPrints(Duration.parse(defaultFormatString), "1h 30m")
        assertPrints(Duration.parse(singleUnitFormatString), "1h 30m")
        assertFails { Duration.parse(invalidFormatString) }
        assertPrints(Duration.parseOrNull(invalidFormatString), "null")
    }

    @Sample
    fun parseIsoString() {
        val isoFormatString = "PT1H30M"
        val defaultFormatString = "1h 30m"

        assertPrints(Duration.parseIsoString(isoFormatString), "1h 30m")
        assertFails { Duration.parseIsoString(defaultFormatString) }
        assertPrints(Duration.parseIsoStringOrNull(defaultFormatString), "null")
    }

}