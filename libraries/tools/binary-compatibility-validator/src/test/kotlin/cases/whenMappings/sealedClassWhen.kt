package cases.whenMappings

sealed class SampleSealed {
    class A : SampleSealed()
    class B : SampleSealed()
    class C : SampleSealed()
}

fun SampleSealed.deacronimize() = when (this) {
    is SampleSealed.A -> "Apple"
    is SampleSealed.B -> "Biscuit"
    is SampleSealed.C -> "Cinnamon"
}


inline fun SampleSealed.switch(thenA: () -> Unit, thenB: () -> Unit, thenC: () -> Unit) = when (this) {
    is SampleSealed.C -> thenC()
    is SampleSealed.B -> thenB()
    is SampleSealed.A -> thenA()
}
