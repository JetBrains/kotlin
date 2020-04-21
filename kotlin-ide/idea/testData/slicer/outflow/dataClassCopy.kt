// FLOW: OUT

data class DataClass(val value1: Int, val value2: Int)

fun foo(dataClass: DataClass) {
    val v = dataClass.value1
}

fun bar(<caret>value: Int, dataClass: DataClass) {
    foo(dataClass.copy(value1 = value))
}