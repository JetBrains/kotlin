package foo

fun box(): Boolean {

    val a = "bar";
    var b = "foo";
    b = a;
    return (b == "bar");
}

