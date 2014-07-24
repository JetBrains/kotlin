//file
class Outer {
    private static class Nested1 {
        public Nested1(){}

        public Nested1(int a) {
            this();
        }

        protected Nested1(char c) {
            this();
        }

        private Nested1(boolean b) {
            this();
        }
    }

    protected static class Nested2 {
        public Nested2(){}

        public Nested2(int a) {
            this();
        }

        protected Nested2(char c) {
            this();
        }

        private Nested2(boolean b) {
            this();
        }

    }

    static class Nested3 {
        public Nested3(){}

        public Nested3(int a) {
            this();
        }

        protected Nested3(char c) {
            this();
        }

        private Nested3(boolean b) {
            this();
        }
    }

    public static class Nested4 {
        public Nested4(){}

        public Nested4(int a) {
            this();
        }

        protected Nested4(char c) {
            this();
        }

        private Nested4(boolean b) {
            this();
        }
    }

    static void foo() {
        Nested1 nested1 = new Nested1(1);
        Nested2 nested2 = new Nested2(2);
        Nested3 nested3 = new Nested3(3);
        Nested4 nested4 = new Nested4(4);
    }
}
