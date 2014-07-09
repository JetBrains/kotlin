//file
class X {
    public int length = 5;
}

class C{
    void foo(X x) {
        for (int i = 0; i < x.length; i++) {
            System.out.print(i);
        }
    }
}
