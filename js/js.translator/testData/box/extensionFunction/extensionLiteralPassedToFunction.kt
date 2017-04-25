// EXPECTED_REACHABLE_NODES: 489
package foo

fun apply(i: Int, f: Int.(Int) -> Int) = i.f(1);

fun box(): String {
    return if (apply(1, { i -> i + this }) == 2) "OK" else "fail"
}