class A {
    A(Nested nested) {
    }

    A() {
        this(new Nested(Nested.FIELD));
    }

    static class Nested {
        Nested(int p){}

        public static final int FIELD = 0;
    }
}