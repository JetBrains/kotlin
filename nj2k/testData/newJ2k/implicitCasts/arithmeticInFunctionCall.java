class A {
    void acceptDouble(double d) {}
    void acceptDoubleBoxed(Double d) {}

    public void conversion() {
        int a = 10;
        float b = 0.7f;
        acceptDouble(a * b)
        acceptDoubleBoxed(a * b)
    }
}