public final class A {
    public A() { /* compiled code */ }

    public static final class B {
        public B() { /* compiled code */ }

        public static final class I {
            public static final A.B.I INSTANCE;

            private I() { /* compiled code */ }
        }

        public static final class II {
            public static final A.B.II INSTANCE;

            private II() { /* compiled code */ }
        }
    }

    public static final class C {
        public static final A.C INSTANCE;

        private C() { /* compiled code */ }

        public static final class D {
            public static final A.C.D INSTANCE;

            private D() { /* compiled code */ }

            public static final class G {
                public static final A.C.D.G INSTANCE;

                private G() { /* compiled code */ }
            }
        }
    }
}