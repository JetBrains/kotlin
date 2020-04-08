interface I {
    boolean isSomething1();

    Boolean isSomething2();

    int isSomething3();

    boolean isSomething4();
    void setSomething4(boolean value);

    boolean isSomething5();
    void setSomething5(boolean value);

    boolean getSomething6();
    void setSomething6(boolean value);
}

abstract class C implements I {
    @Override
    public boolean isSomething1() {
        return true;
    }

    public void setSomething1(boolean b) {
    }

    @Override
    public boolean isSomething4() {
        return false;
    }

    @Override
    public void setSomething5(boolean value) {
    }

    @Override
    public void setSomething6(boolean value) {
    }
}