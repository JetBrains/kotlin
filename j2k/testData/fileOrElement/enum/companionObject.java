public enum TestEnum {
    A,
    B;

    public static TestEnum parse() { return A; }
}

class Go {
    void fn() {
        TestEnum x = TestEnum.parse();
    }
}