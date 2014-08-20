class Base {
    Base(Nested nested){}

    static class Nested {
        Nested(int p){}

        public static final int FIELD = 0;
    }
}

class Derived extends Base {
    Derived() {
        super(new Nested(Nested.FIELD));
    }
}