class `:)` {
    lateinit val f: String
}

// Commented declarations won't compile with the current Kotlin
class Test {
    class `(^_^)`

    lateinit val simpleName: String
    lateinit val `strange name`: String
//    lateinit val strangeType: List<`!A@`>

    fun simpleFun() {}

    @Anno(name = "Woofwoof", size = StrangeEnum.`60x60`, `A B` = "S")
    fun simpleFun2(a: String, b: String) {}

    fun `strange!Fun`() {}
//    fun strangeFun2(a: String, b: `A()B()`) {}
//    fun strangeFun3(a: String, b: `A B`) {}
    fun strangeFun4(a: String, `A()B()`: String) {}
//    fun strangeFun5(a: `A B`.C) {}
}

enum class StrangeEnum(val size: String) {
    `60x60`("60x60"),
    `70x70`("70x70"),
    `80x80`("80x80"),
    InvalidFieldName("0x0") // Workaround to pass javac analysis
}

annotation class Anno(val size: StrangeEnum, val name: String, val `A B`: String)

class `!A@`
class `A()B()`
class `A B` {
    class C
}

// EXPECTED_ERROR(other:-1:-1) '60x60' is an invalid Java enum value name