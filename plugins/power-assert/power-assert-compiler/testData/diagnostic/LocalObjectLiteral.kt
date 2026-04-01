// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-85178

import kotlin.powerassert.*

@PowerAssert.Ignore
interface AssertScope<out T> {
    fun collect(message: String?, explanation: Explanation?)
    fun fail(message: String?, explanation: Explanation?)
}

@PowerAssert
fun <T> assertThat(subject: T, block: AssertScope<T>.() -> Unit) {
    val scope = object : AssertScope<T> {
        override fun collect(message: String?, explanation: Explanation?) {
        }

        override fun fail(message: String?, explanation: Explanation?) {
            collect(message, explanation) // 'collect' doesn't have a 'CallableId'.
        }
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, elvisExpression, equalityExpression,
funWithExtensionReceiver, functionDeclaration, functionalType, infix, interfaceDeclaration, lambdaLiteral, localProperty,
nullableType, out, outProjection, override, primaryConstructor, propertyDeclaration, stringLiteral, thisExpression,
tryExpression, typeParameter, typeWithExtension, vararg */
