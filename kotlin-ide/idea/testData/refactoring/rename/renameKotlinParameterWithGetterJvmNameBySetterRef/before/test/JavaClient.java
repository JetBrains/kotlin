package test;

class Test {
    {
        new A().getFoo();
        new A()./*rename*/setFirst(1);
    }
}