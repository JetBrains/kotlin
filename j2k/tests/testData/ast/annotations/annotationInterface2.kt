annotation class Anon(public val s: String = "a", public val stringArray: Array<String> = array("a", "b"), public vararg val intArray: Int)

Anon(intArray = *intArray(1, 2))
class A