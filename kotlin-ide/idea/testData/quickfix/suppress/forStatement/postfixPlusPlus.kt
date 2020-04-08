// "Suppress 'USELESS_CAST' for statement " "true"

fun foo() {
    val arr = IntArray(1)
    arr[1 a<caret>s Int]++
}
