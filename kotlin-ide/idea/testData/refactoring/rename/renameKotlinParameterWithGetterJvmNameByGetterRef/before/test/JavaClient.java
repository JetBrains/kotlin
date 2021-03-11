package test;

class Test {
    {
        new A()./*rename*/getFoo();
        new A().setFirst(1);
    }
}