class X {
    public int size() { return 5; }
}

class C{
    void foo(X x) {
        for (int i = 0; i < x.size(); i++) {
            System.out.print(i);
        }
    }
}
