/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.time

import samples.*

import kotlin.time.*

class Durations {

    @Sample
    fun toIsoString() {
        assertPrints(Duration.nanoseconds(25).toIsoString(), "PT0.000000025S")
        assertPrints(Duration.milliseconds(120.3).toIsoString(), "PT0.120300S")
        assertPrints(Duration.seconds(30.5).toIsoString(), "PT30.500S")
        assertPrints(Duration.minutes(30.5).toIsoString(), "PT30M30S")
        assertPrints(Duration.seconds(86420).toIsoString(), "PT24H0M20S")
        assertPrints(Duration.days(2).toIsoString(), "PT48H")
        assertPrints(Duration.ZERO.toIsoString(), "PT0S")
        assertPrints(Duration.INFINITE.toIsoString(), "PT2147483647H")
    }

    @Sample
    fun toStringDefault() {
        assertPrints(Duration.days(45), "45.0d")
        assertPrints(Duration.days(1.5), "36.0h")
        assertPrints(Duration.minutes(1230), "20.5h")
        assertPrints(Duration.minutes(920), "920m")
        assertPrints(Duration.seconds(1.546), "1.55s")
        assertPrints(Duration.milliseconds(25.12), "25.1ms")
    }

    @Sample
    fun toStringDecimals() {
        assertPrints(Duration.minutes(1230).toString(DurationUnit.DAYS, 2), "0.85d")
        assertPrints(Duration.minutes(1230).toString(DurationUnit.HOURS, 2), "20.50h")
        assertPrints(Duration.minutes(1230).toString(DurationUnit.MINUTES), "1230m")
        assertPrints(Duration.minutes(1230).toString(DurationUnit.SECONDS), "73800s")
    }



}