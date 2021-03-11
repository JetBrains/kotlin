class C extends A<String> {
    C(String s) {
        super(s);
    }

    @Override
    public String getFoo() {
        return super.getFoo();
    }

    @Override
    public void setFoo(String s) {
        super.setFoo(s);
    }
}
