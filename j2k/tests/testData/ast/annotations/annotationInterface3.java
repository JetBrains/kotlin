@interface Anon {
    String value();

    enum E {
        A, B
    }

    E field = E.A;
}

@Anon("a")
interface I {
    Anon.E e = Anon.field;
}
