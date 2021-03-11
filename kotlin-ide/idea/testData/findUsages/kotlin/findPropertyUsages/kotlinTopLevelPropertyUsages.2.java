package client;

import server.KotlinTopLevelPropertyUsages_0Kt;

class JClient {
    void fooBar() {
        System.out.println("foo = " + KotlinTopLevelPropertyUsages_0Kt.getFoo());
        System.out.println("length: " + KotlinTopLevelPropertyUsages_0Kt.getFoo().length());
        KotlinTopLevelPropertyUsages_0Kt.setFoo("");
    }
}