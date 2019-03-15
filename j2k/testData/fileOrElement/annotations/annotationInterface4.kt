internal annotation class Anon(vararg val value: String, val x: Int = 1)

@Anon("a", "b")
internal interface I1

@Anon("c", "d", x = 1)
internal interface I2

@Anon("c", "d", x = 1)
internal interface I3

@Anon(value = ["c", "d"])
internal interface I4
