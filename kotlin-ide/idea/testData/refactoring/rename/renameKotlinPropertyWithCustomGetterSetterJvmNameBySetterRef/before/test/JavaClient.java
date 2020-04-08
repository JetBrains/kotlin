package test;

class Test {
    {
        new A().foo();
        new A()./*rename*/bar(1);
    }
}