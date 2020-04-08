class X<A> {

}

interface Foo<A, B> {
    A <caret>foo(A a, B b);
}

class SamTest {
    static <A, B> void test(Foo<A, B> foo) {

    }
}