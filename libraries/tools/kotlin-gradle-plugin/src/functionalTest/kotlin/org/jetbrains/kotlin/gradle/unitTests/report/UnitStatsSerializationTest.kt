/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.report

import com.google.gson.GsonBuilder
import org.jetbrains.kotlin.util.*
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.assertEquals

class UnitStatsSerializationTest {
    @Test
    fun validateAllProperties() {
        val moduleStats = UnitStats(
            name = "all_properties",
            outputKind = null,
            timeStampMs = LocalDateTime.of(2025, 6, 19, 20, 30, 56).toInstant(ZoneOffset.UTC).toEpochMilli(),
            platform = PlatformType.Native,
            compilerType = CompilerType.K1andK2,
            hasErrors = true,
            filesCount = 10,
            linesCount = 11,

            initStats = Time(1_000_000L, 1_000_001L, 1_000_002L),
            analysisStats = Time(2_000_000L, 2_000_001L, 2_000_002L),
            translationToIrStats = Time(3_000_000L, 3_000_001L, 3_000_002L),
            irPreLoweringStats = Time(3_500_000L, 3_500_001L, 3_500_002L),
            irSerializationStats = Time(3_600_000L, 3_600_001L, 3_600_002L),
            klibWritingStats = Time(3_700_000L, 3_700_001L, 3_700_002L),
            irLoweringStats = Time(4_000_000L, 4_000_001L, 4_000_002L),
            backendStats = Time(5_000_000L, 5_000_001L, 5_000_002L),
            dynamicStats = listOf(
                DynamicStats(PhaseType.IrPreLowering, "Dynamic 1", Time(5_100_000L, 5_100_001L, 5_100_002L)),
                DynamicStats(PhaseType.IrLowering, "Dynamic 2", Time(5_200_000L, 5_200_001L, 5_200_002L)),
            ),
            findJavaClassStats = SideStats(1, Time(6_000_000L, 6_000_001L, 6_000_002L)),
            findKotlinClassStats = SideStats(2, Time(7_000_000L, 7_000_001L, 7_000_002L)),
            gcStats = listOf(
                GarbageCollectionStats("GC", 8_000L, 16),
                GarbageCollectionStats("GC 2", 18_000L, 4),
            ),
            jitTimeMillis = 9_000L,
            klibElementStats = listOf(
                KlibElementStats("KLIB directory cumulative size", 100_000),
                KlibElementStats("KLIB directory cumulative size/Manifest file", 40_000),
                KlibElementStats("KLIB directory cumulative size/IR files", 60_000)
            ),
        )

        val serialized = UnitStatsJsonDumper.dump(moduleStats)

        val deserialized = GsonBuilder().create().fromJson(serialized, UnitStats::class.java)

        assertEquals(moduleStats, deserialized)
    }
}