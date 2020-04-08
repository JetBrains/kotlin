interface Test {
    fun foo(): Test
}

fun test(t: Test) {
    t    .    foo()
    t()    .   foo()
    t()    !!    .    foo()    .foo()
    (   (    t()    !!)    .    foo()    .foo()     .foo())       .foo()
    t.
            foo()

    t.


    foo()

    t



            .foo()


    t.foo()    .    foo()


    t    ?.    foo()

    t?.
    foo()

    t?.


    foo()

    t



            ?.foo()

    t?.foo()    ?.    foo()
}