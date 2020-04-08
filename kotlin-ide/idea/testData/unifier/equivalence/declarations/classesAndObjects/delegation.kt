// DISABLE-ERRORS
fun foo() {
    interface T
    open class Z(p: Int): T;

    {
        <selection>class A: T by Z(1)</selection>
    }

    {
        class B: Z(1), T
    }

    {
        class C: Z(1)
    }

    {
        class D: T by Z(1)
    }

    {
        class E: T by Z(2)
    }
}