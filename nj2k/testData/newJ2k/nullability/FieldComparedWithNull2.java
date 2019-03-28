//file
class C {
    private String s;

    public C(String s) {
        this.s = s;
    }

    void foo() {
        if (s != null) {
            System.out.print("not null");
        }
    }
}
