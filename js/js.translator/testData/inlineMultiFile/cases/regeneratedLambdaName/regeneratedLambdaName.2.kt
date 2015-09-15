/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/lambdaTransformation/regeneratedLambdaName.2.kt
 */

package test

import kotlin.InlineOption.*

inline fun <R> call(crossinline f: () -> R) : R {
    return {f()} ()
}
