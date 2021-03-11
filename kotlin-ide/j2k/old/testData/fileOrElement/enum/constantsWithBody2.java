public enum E {
    A(1) {
        @Override
        void bar() {
            foo(this.p);
        }
    },

    B(2) {
        @Override
        void bar() {
        }
    };

    private int p;

    E(int p) {
        this.p = p;
    }

    void foo(int p) {}

    abstract void bar();
}