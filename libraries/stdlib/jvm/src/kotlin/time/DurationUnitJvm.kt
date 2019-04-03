/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass()
@file:kotlin.jvm.JvmName("DurationUnitKt")

package kotlin.time

//Actual typealias 'DurationUnit' has no corresponding expected declaration
//The following declaration is incompatible because modality is different:
//    public final expect enum class DurationUnit : Enum<DurationUnit>

@Suppress("ACTUAL_WITHOUT_EXPECT")
public actual typealias DurationUnit = java.util.concurrent.TimeUnit


public actual fun convertDurationUnit(value: Double, sourceUnit: DurationUnit, targetUnit: DurationUnit): Double {
    val sourceInTargets = targetUnit.convert(1, sourceUnit)
    if (sourceInTargets > 0)
        return value * sourceInTargets

    val otherInThis = sourceUnit.convert(1, targetUnit)
    return value / otherInThis
}