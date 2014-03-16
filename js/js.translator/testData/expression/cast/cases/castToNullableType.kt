package foo

class A

fun box(): String {
    val a = null
    val s = a as A?
    if (s != null) return "Failed when try cast Nullable with null value to Nullable"

    val b: A? = A()
    val n = b as A?
    if (n != b) return "Failed when try cast Nullable with not null value to Nullable"

    val c = A()
    val m = c as A?
    if (m != c) return "Failed when try cast NotNull to Nullable"

    return "OK"
}
