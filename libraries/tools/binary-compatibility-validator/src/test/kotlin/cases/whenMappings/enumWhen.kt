package cases.whenMappings

enum class SampleEnum {
    A,
    B,
    C
}

fun SampleEnum.deacronimize() = when (this) {
    SampleEnum.A -> "Apple"
    SampleEnum.B -> "Biscuit"
    SampleEnum.C -> "Cinnamon"
}


inline fun SampleEnum.switch(thenA: () -> Unit, thenB: () -> Unit, thenC: () -> Unit) = when (this) {
    SampleEnum.C -> thenC()
    SampleEnum.B -> thenB()
    SampleEnum.A -> thenA()
}
