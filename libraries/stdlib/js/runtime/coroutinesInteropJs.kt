/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(JsIntrinsic::class)
package kotlin.coroutines.intrinsics

import kotlin.coroutines.Continuation
import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.js.Promise

// We use a single character name here just for reducing the produced code size.
// Sure, terser and other minifiers can handle it well, however, the size of input for the minifiers also does matter.
// Since, the function is called quite often, we want to keep it as short as possible.
// Note: `$` could be replaced with any other single character.
@JsName("$")
@UsedFromCompilerGeneratedCode
internal suspend fun <T> suspendLambdaRun(value: dynamic): T {
    // Please don't change the condition without strong reasons
    // this specific check shows the best benchmarking results across all browsers
    // between different approaches.
    // You can check the micro-benchmark here: https://jsbm.dev/L2qWRbhEQABha
    if (value.constructor === Promise::class.js) {
        return await(value)
    } else {
        return jsYieldStar(value)
    }
}

private val continuationSymbol = Continuation::class.js.asDynamic().Symbol

// The return type of this function is either the GeneratorIterator (if the continuation is provided) or Promise<T>
// if it's not. We use this trick to consume suspend lambdas differently on the Kotlin side and on the JavaScript/TypeScript side.
@UsedFromCompilerGeneratedCode
internal fun <T> orPromise(continuation: dynamic, lambda: dynamic): dynamic {
    // Instead of using `is` Continuation, we just check for the symbol to make it more performant (it's a micro-optimization)
    // Is check does the same but just in a separate function getting the symbol out of constructor and checking it in the instance.
    // We just speed up this process a bit since we've moved the symbol to a variable
    if (continuation != VOID && continuation[continuationSymbol]) {
        return lambda(continuation)
    } else {
        return promisify<T>(lambda)
    }
}
