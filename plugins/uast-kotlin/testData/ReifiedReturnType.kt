inline fun <T> function1(t: T, i: Int, s: String) {}
inline fun <T> function2(t: T, i: Int, s: String): T = t
inline fun <T : CharSequence> function2CharSequence(t: T, i: Int, s: String): T = t
inline fun <reified T> function3(t: T, i: Int, s: String) {}
inline fun <reified T> function4(t: T, i: Int, s: String): T = t
inline fun <reified T> function5(t: T, i: Int, s: String): Int = 42
inline fun <reified T : Activity> T.function6(t: T, i: Int, s: String): T = t
inline fun <reified T> function7(t: T, i: Int, s: String): T = t
private inline fun <reified T> function8(t: T, i: Int, s: String): T = t
internal inline fun <reified T> function9(t: T, i: Int, s: String): T = t
public inline fun <reified T> function10(t: T, i: Int, s: String): T = t
inline fun <reified T> T.function11(t: T, i: Int, s: String): T = t
inline fun <reified T : CharSequence> T.function11CharSequence(t: T, i: Int, s: String): T = t
inline fun <reified T : CharSequence, reified B : T> T.function12CharSequence(t: B, i: T, s: String): B = t

fun <T, B> copyWhenGreater(list: List<T>, threshold: T, threshold2: B): B
        where T : CharSequence,
              T : Comparable<T>,
              B : T {
    return threshold2
}

class Foo<T> {
    inline fun <reified Z : T> foo(): Z {
        TODO()
    }
}

