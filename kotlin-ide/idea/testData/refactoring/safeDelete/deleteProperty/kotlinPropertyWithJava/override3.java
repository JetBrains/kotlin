class B extends A {
    void bar() {
        setFoo("");
        System.out.println(getFoo());
    }

    @Override
    public String getFoo() {
        return "foo";
    }

    @Override
    public void setFoo(String value) {

    }
}