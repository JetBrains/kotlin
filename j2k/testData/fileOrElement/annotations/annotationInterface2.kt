internal annotation class Anon(val s: String = "a", val stringArray: Array<String> = arrayOf("a", "b"), val intArray: IntArray)

@Anon(intArray = intArrayOf(1, 2))
internal class A
