package foo

fun test(f: (Int) -> Boolean, p: Int) = f(p)

fun box(): Boolean {
    if (!test({ it + 1 == 2 }, 1)) return false;

    if (!test({ it > 1 }, 3)) return false;

    return (test({ ((it < 1) == false) }, 1))

}