fun foo(x: Int) = "int: $x"

fun foo(x: String) = "string: $x"

inline fun bar(x: Int) = foo(x)

inline fun bar(x: String) = foo(x)

// CHECK_BREAKS_COUNT: function=box count=0
// CHECK_LABELS_COUNT: function=box name=$l$block count=0
fun box(): String {
    val a = bar(23)
    if (a != "int: 23") return "fail1: $a"

    val b = bar("qqq")
    if (b != "string: qqq") return "fail2: $b"

    return "OK"
}