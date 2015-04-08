annotation class Anon(public vararg val value: String, public val x: Int = 1)

Anon("a", "b")
trait I1

Anon("c", "d", x = 1)
trait I2

Anon("c", "d", x = 1)
trait I3

Anon(value = *array("c", "d"))
trait I4