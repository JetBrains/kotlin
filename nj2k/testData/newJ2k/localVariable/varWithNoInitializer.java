//file
class C {
    int foo(boolean p) {
        int a;
        int b;
        a = 10;
        b = 5;
        if (p) a = 5; else b = 10;
        return a + b;
    }
}
