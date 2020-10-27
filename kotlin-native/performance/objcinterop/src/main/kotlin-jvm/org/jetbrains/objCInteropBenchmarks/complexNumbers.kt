/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.complexNumbers

actual class ComplexNumber() {}

actual class ComplexNumbersBenchmark actual constructor() {
    actual fun generateNumbersSequence(): List<ComplexNumber> {
        error("Benchmark generateNumbersSequence is unsupported on JVM!")
    }

    actual fun sumComplex() {
        error("Benchmark sumComplex is unsupported on JVM!")
    }

    actual fun subComplex() {
        error("Benchmark subComplex is unsupported on JVM!")
    }
    actual fun classInheritance() {
        error("Benchmark classInheritance is unsupported on JVM!")
    }
    actual fun categoryMethods() {
        error("Benchmark categoryMethods is unsupported on JVM!")
    }
    actual fun stringToObjC() {
        error("Benchmark stringToObjC is unsupported on JVM!")
    }
    actual fun stringFromObjC() {
        error("Benchmark stringToObjC is unsupported on JVM!")
    }
    actual fun fft() {
        error("Benchmark fft is unsupported on JVM!")
    }
    actual fun invertFft() {
        error("Benchmark invertFft is unsupported on JVM!")
    }
}
