fun a(): Boolean {
    val a = 3
    val b = 4
    <warning descr="SSR">when {
        a == b -> return true
        else ->  return false
    }</warning>
}