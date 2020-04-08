// FIR_COMPARISON

interface A {
    fun foo()
}

interface B : A {
    fun bar()
}

interface C

interface D : A

fun test(a: A?) {
    if (a != null && a is B?) {
        <info descr="Smart cast to B">a</info>.bar()
    }

    if (a is B && a is C) {
        <info descr="Smart cast to B">a</info>.foo()
    }

    if (a is B? && a is C?) {
        <info descr="Smart cast to B?">a</info><info>?.</info>bar()
    }

    a<info>?.</info>foo()
    if (a is B? && a is C?) {
        a<info>?.</info>foo()
    }

    if (a is B && a is D) {
        //when it's resolved, the message should be 'Smart cast to A'
        <info>a</info>.<error>foo</error>
    }
}
