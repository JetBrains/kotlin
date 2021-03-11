// "Change the signature of lambda expression" "true"
// SHOULD_FAIL_WITH: "'x' is used in declaration body"
// DISABLE-ERRORS

fun f(x: Int, y: Int, z : (Int, Int?, Any) -> Int) {
    f(1, 2, {<caret>x: Int -> x});
}
