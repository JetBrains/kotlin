fun foo() {
    val nv1 = 1
    val nv2 = "prefix $nv1" + <caret>"postfix"
}
