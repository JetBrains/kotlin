class J extends K {
    @Override
    public void foo(@NotNull Function2<? super Integer, ? super Boolean, String> f) {
        super.foo(f);
    }
}