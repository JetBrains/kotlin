package client;

import server.*;

class Client {
    void fooBar() {
        A<String> a = new A<String>("");
        a.setFoo("a");
        System.out.println("a.foo = " + a.getFoo());

        B b = new B();
        b.setFoo("b");
        System.out.println("b.foo = " + b.getFoo());
    }
}