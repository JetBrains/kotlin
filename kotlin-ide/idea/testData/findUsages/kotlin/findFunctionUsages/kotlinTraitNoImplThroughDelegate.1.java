package client;

import server.TraitWithDelegatedNoImpl;

public class JClient {
    public static void bar(TraitWithDelegatedNoImpl some) {
        some.foo();
    }
}