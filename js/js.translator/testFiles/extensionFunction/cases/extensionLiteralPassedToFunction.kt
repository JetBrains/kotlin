package foo

fun apply(i: Int, f: Int.(Int) -> Int) = i.f(1);

fun box(): Boolean {
    return apply(1, { i -> i + this }) == 2
}