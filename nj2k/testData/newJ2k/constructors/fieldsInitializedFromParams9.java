class C {
    private final String string;

    public C(String s, int a) {
        this.string = s;
    }

    public C(String s) {
        this(s, s.length());
    }
}
