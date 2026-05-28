/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.complexNumbers

import kotlinx.benchmark.Blackhole

expect class ComplexNumber

expect class ComplexNumbersBenchmark() {
    fun generateNumbersSequence(bh: Blackhole)
    fun sumComplex(bh: Blackhole)
    fun subComplex(bh: Blackhole)
    fun classInheritance(bh: Blackhole)
    fun categoryMethods(bh: Blackhole)
    fun stringToObjC(bh: Blackhole)
    fun stringFromObjC(bh: Blackhole)
    fun fft(bh: Blackhole)
    fun invertFft(bh: Blackhole)
}
