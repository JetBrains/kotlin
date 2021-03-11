class Foo1 { /// M
    val x: String /// F, L
        get() = "foo" /// M
}

class Foo2 { /// M
    val x: String = "foo" /// F, L
        get() = field + "x" /// M
}