// KJS_WITH_FULL_RUNTIME
package foo

fun box(): String {
    for (i in 0..5);

    val r = 0..5
    for (i in r);

    for (i in arrayOf(1, 2, 3));

    for (i in arrayOf(1, 2, 3).asList());

    return "OK"
}
