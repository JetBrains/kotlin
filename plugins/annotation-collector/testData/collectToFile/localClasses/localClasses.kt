package org.test

public class SomeClass {

    public fun someFunction() {
        @Deprecated class LocalClass {

            public Deprecated var annotatedProperty: String? = null

            public inline fun annotatedFunction() {

            }

        }
    }

}