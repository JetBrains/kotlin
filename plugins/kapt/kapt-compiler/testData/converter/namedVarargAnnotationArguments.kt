// CORRECT_ERROR_TYPES

// FILE: app/B.java
package app;

public class B {
    public static class id {
        public final static int textView = 200;
    }

    public final static int a3 = 2;
}

// FILE: test.kt
package test

import app.B

import kotlin.reflect.KClass

annotation class VarargValue(vararg val ids: Int)
annotation class VarargValueString(vararg val ids: String)
annotation class VarargArrays(vararg val ids: IntArray)
annotation class VarargClasses(vararg val ids: KClass<*>)

class MyActivity {
    @VarargValue(ids = *intArrayOf(B.id.textView, B.a3))
    fun realSpread() {}

    @VarargValueString(ids = emptyArray())
    fun emptyArrayCall() {}

    @VarargArrays(ids = [[1, 2], [3]])
    fun nestedArrays() {}

    @VarargValue(ids = B.a3)
    fun namedNonArray() {}

    @VarargClasses(ids = *arrayOf(Unresolved::class))
    fun realSpreadClassLiteral() {}
}
