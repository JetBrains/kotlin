fun a(): Boolean {
    val a: Any = 3
    <warning descr="SSR">when(a) {
        is Int -> return true
        is String -> return true
        else ->  return false
    }</warning>
}