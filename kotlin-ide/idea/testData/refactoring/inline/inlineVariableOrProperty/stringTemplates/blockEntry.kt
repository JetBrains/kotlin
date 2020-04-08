fun foo() {
    val a = 1
    val <caret>x = "_$a:${a + 1}_"
    val y = "__x=${x}__"
}