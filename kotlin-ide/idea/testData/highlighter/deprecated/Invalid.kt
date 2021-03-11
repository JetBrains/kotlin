// IGNORE_FIR

@Deprecated(<error>)</error>
fun foo() {}
@Deprecated(<error>false</error>)
fun boo() {}

fun far() = <warning descr="[DEPRECATION] 'foo(): Unit' is deprecated. ">foo</warning>()

fun bar() = <warning descr="[DEPRECATION] 'boo(): Unit' is deprecated. ">boo</warning>()

// NO_CHECK_INFOS
// NO_CHECK_WEAK_WARNINGS
