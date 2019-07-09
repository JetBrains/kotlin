//file
class C {
    public String s = "";
}

class D {
    void foo(C c) {
        if (null == c.s) {
            System.out.println("null");
        }
    }
}
