/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package codegen.function.kt55776

import kotlinx.cinterop.*
import kotlin.test.*

// region: KT-55776
// Checker issues: type T of codegen.function.kt55776.callCFunction  of return value is not supported here: doesn't correspond to any C type
@Suppress("UNCHECKED_CAST")
fun <T : Number> callCFunction(): T {
    val cFunction: CPointer<CFunction<() -> T>> = staticCFunction<T> { 42 as T }
    return cFunction()
}

@Test
fun callChecker() {
    assertEquals(callCFunction(), 42)
}
// endregion

// region: Fails with exception in the lowering
// org.jetbrains.kotlin.backend.konan.lower.InteropTransformer.visitCall(InteropLowering.kt:1113)
fun <T: Number> getCFun(): CPointer<CFunction<() -> T>> = staticCFunction<T> { 42 as T }

@Test
fun callChecker2() {
    assertEquals(getCFun<Int>()(), 42)
}
// endregion

// region: Checker issues: type T of codegen.function.kt55776.getCFun2  of callback parameter 1 is not supported here: doesn't correspond to any C type
@Suppress("UNCHECKED_CAST")
fun <T: Number> getCFun2(): CPointer<CFunction<(T) -> T>> = staticCFunction<T, T> { _ -> 42 as T }

@Test
fun callChecker3() {
    assertEquals(getCFun2<Int>()(13), 42)
}
// endregion

// region: Fails with SIGSEGV due to the stack overflow but Cheker doesn't complain
fun <T: Number> getCFunFromCFun(): CPointer<CFunction<() -> T>> = staticCFunction<CPointer<CFunction<() -> T>>> { getCFunFromCFun<T>() }()

@Test
fun callChecker4() {
    getCFunFromCFun<Int>()()
}
// endregion

// region: Passes
fun getF(): CPointer<CFunction<() -> Int>> = staticCFunction<Int> { 42 }

@Suppress("UNCHECKED_CAST")
fun <T: Number> getCFunFromCFun(): CPointer<CFunction<() -> T>> = getF() as CPointer<CFunction<() -> T>>

@Test
fun callChecker5() {
    assertEquals(getCFunFromCFun<Int>()(), 42)
}
// endregion