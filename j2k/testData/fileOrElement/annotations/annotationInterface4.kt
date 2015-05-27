annotation class Anon(public vararg val value: String, public val x: Int = 1)

Anon("a", "b")
interface I1

Anon("c", "d", x = 1)
interface I2

Anon("c", "d", x = 1)
interface I3

Anon(value = *arrayOf("c", "d"))
interface I4
