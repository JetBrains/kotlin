package org.jetbrains.kotlin.stats

import org.jetbrains.kotlin.util.CompilerType
import org.jetbrains.kotlin.util.GarbageCollectionStats
import org.jetbrains.kotlin.util.PlatformType
import org.jetbrains.kotlin.util.SideStats
import org.jetbrains.kotlin.util.Time
import org.jetbrains.kotlin.util.UnitStats
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

object TestData {
    val timestampMsFromDateThatLooksReal = LocalDateTime.of(2025, 6, 19, 20, 30, 56).toInstant(ZoneOffset.UTC).toEpochMilli()

    val moduleStats0: UnitStats = createDummyModuleStats(1, CompilerType.K1, hasErrors = false)
    val moduleStats1: UnitStats = createDummyModuleStats(2, CompilerType.K2, hasErrors = false)
    val moduleStats2: UnitStats = createDummyModuleStats(3, CompilerType.K2, hasErrors = true)

    private const val TIME_STAMP_MODULE_NAME = "time_stamp"
    val timeStampStats0: UnitStats = createDummyModuleStats(1, name = TIME_STAMP_MODULE_NAME)
    val timeStampStats1: UnitStats = createDummyModuleStats(2, name = TIME_STAMP_MODULE_NAME)
    val timeStampStats2: UnitStats = createDummyModuleStats(3, name = TIME_STAMP_MODULE_NAME)

    val moduleStats: ModulesReportsData = ModulesReportsData(listOf(moduleStats0, moduleStats1, moduleStats2).associateBy { it.name!! })

    val timeStampStats: TimestampReportsData = TimestampReportsData(TIME_STAMP_MODULE_NAME, sortedSetOf(timeStampStats0, timeStampStats1, timeStampStats2))

    private fun createDummyModuleStats(
        increment: Int,
        compilerType: CompilerType = CompilerType.K2,
        hasErrors: Boolean = false,
        name: String? = null
    ): UnitStats {
        val elapsedNanosThatLooksReal = TimeUnit.SECONDS.toNanos(increment.toLong())
        val totalTime = Time(
            elapsedNanosThatLooksReal,
            elapsedNanosThatLooksReal - (0.5 * TimeUnit.SECONDS.toNanos(1)).toLong(),
            elapsedNanosThatLooksReal + (0.5 * TimeUnit.SECONDS.toNanos(1)).toLong())
        return UnitStats(
            name = name ?: "module-$increment",
            outputKind = increment.toString(),
            timeStampMs = timestampMsFromDateThatLooksReal + TimeUnit.DAYS.toMillis(increment.toLong()),
            platform = PlatformType.JVM,
            compilerType = compilerType,
            hasErrors = hasErrors,
            filesCount = increment,
            linesCount = increment * 64 + 128,
            initStats = totalTime * 0.1,
            analysisStats = totalTime * 0.4,
            translationToIrStats = totalTime * 0.2,
            irPreLoweringStats = totalTime * 0.01,
            irSerializationStats = totalTime * 0.01,
            klibWritingStats = totalTime * 0.01,
            irLoweringStats = totalTime * 0.01,
            backendStats = totalTime * 0.16,
            findJavaClassStats = SideStats(increment, totalTime * 0.04),
            findKotlinClassStats = SideStats(increment, totalTime * 0.06),
            gcStats = listOf(
                GarbageCollectionStats("gc-$increment", (increment * 100).toLong(), increment.toLong()),
            ),
            jitTimeMillis = increment.toLong(),
        )
    }
}