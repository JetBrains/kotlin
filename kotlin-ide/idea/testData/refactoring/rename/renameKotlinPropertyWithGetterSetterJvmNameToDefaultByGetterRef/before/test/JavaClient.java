package test;

class Test {
    {
        new A()./*rename*/getFoo();
        new A().setBar(1);
    }
}