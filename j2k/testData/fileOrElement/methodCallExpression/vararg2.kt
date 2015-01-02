import javaApi.WithVarargConstructor

class X {
    fun foo() {
        val o1 = WithVarargConstructor(1, *array<Any>("a"))
        val o2 = WithVarargConstructor(2, array<Any>("a"), array<Any>("b"))
        val o3 = WithVarargConstructor(2, "a")
    }
}