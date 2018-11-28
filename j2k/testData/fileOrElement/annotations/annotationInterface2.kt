internal annotation class Anon(val s: String = "a", val stringArray: Array<String> = ["a", "b"], val intArray: IntArray)

@Anon(intArray = [1, 2])
internal class A
