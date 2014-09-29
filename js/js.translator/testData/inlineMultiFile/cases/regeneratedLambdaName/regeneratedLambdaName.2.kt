/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/lambdaTransformation/regeneratedLambdaName.2.kt
 */

package test

import kotlin.InlineOption.*

inline fun <R> call(inlineOptions(ONLY_LOCAL_RETURN) f: () -> R) : R {
    return {f()} ()
}
