// KT-56190 K2 does not emit const initializers
// MUTED_WHEN: K2
package test

enum class Weapon {
    ROCK,
    PAPER,
    SCISSORS
}

val byteConst: Byte = 10
val shortConst: Short = 20
val intConst: Int = 30
val longConst: Long = 40
val charConst: Char = 'A'
val stringConst: String = "abcd"
val booleanConst: Boolean = true
val floatConst: Float = 2.0f
val doubleConst: Double = 3.0
val enumConst: Weapon? = Weapon.ROCK
val arrayConst: Any = byteArrayOf(1,2)

val a = 10
val b = a + 20

class Class {
    val byteConst: Byte = 10
    val shortConst: Short = 20
    val intConst: Int = 30
    val longConst: Long = 40
    val charConst: Char = 'A'
    val stringConst: String = "abcd"
    val booleanConst: Boolean = true
    val floatConst: Float = 2.0f
    val doubleConst: Double = 3.0
    val enumConst: Weapon? = Weapon.ROCK
    val arrayConst: Any = byteArrayOf(1,2)
    val a = 10
    val b = a + 20
}

