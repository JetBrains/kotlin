class C {
    private final int p;

    public C(int p) {
        this.p = p
        System.out.println(p++);
        System.out.println(p);
    }
}
