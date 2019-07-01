class Base {
    Base(Object o, int l){}
}

class C extends Base {
    private final String string;

    public C(String s) {
        super(s, s.length());
        this.string = s;
    }
}
