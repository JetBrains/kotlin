import javaApi.WithVarargConstructor

internal class X {
    fun foo() {
        val o1 = WithVarargConstructor(1, *arrayOf<Any>("a"))
        val o2 = WithVarargConstructor(2, arrayOf<Any>("a"), arrayOf<Any>("b"))
        val o3 = WithVarargConstructor(2, "a")
    }
}