fun foo1() {}

@Suppress("DEPRECATED_SINCE_KOTLIN_OUTSIDE_KOTLIN_SUBPACKAGE")
@Deprecated("")
@DeprecatedSinceKotlin(warningSince = "1.0")
fun foo2() {}

@Suppress("DEPRECATED_SINCE_KOTLIN_OUTSIDE_KOTLIN_SUBPACKAGE")
@Deprecated("")
@DeprecatedSinceKotlin(warningSince = "999.999")
fun foo3() {}

fun foo4() {}

fun test() {
    foo<caret>
}

// ORDER: foo1, foo3, foo4, foo2