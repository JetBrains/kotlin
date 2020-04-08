package test;

class Test {
    {
        new A().getFirst();
        new A()./*rename*/setBar(1);
    }
}