// !specifyLocalVariableTypeByDefault: true
fun foo(list: List<String>) {
    val array: IntArray = IntArray(10)
    for (i: Int in 0..9) {
        array[i] = i
    }

    for (s: String in list) print(s)
}