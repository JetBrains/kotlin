/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass()
@file:kotlin.jvm.JvmName("DurationUnitKt")

package kotlin.time

@SinceKotlin("1.3")
@ExperimentalTime
public actual typealias DurationUnit = java.util.concurrent.TimeUnit

@SinceKotlin("1.3")
@ExperimentalTime
internal actual fun convertDurationUnit(value: Double, sourceUnit: DurationUnit, targetUnit: DurationUnit): Double {
    val sourceInTargets = targetUnit.convert(1, sourceUnit)
    if (sourceInTargets > 0)
        return value * sourceInTargets

    val otherInThis = sourceUnit.convert(1, targetUnit)
    return value / otherInThis
}