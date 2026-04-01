// RUN_PIPELINE_TILL: FRONTEND

import kotlin.powerassert.*

abstract class Base {
    abstract fun test()
}

class Derived : Base() {
    <!POWER_ASSERT_ILLEGAL_OVERRIDE!>@PowerAssert<!>
    override fun test() {}
}

/* GENERATED_FIR_TAGS: classDeclaration, elvisExpression, equalityExpression, funWithExtensionReceiver,
functionDeclaration, functionalType, infix, lambdaLiteral, localProperty, nullableType, outProjection, override,
primaryConstructor, propertyDeclaration, stringLiteral, thisExpression, tryExpression, typeParameter, vararg */
