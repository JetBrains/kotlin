package client;

import server.O;

class Client {
    void fooBar() {
        System.out.println("foo = " + O.INSTANCE.getFoo());
    }
}