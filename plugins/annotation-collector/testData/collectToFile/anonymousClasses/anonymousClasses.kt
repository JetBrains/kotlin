package org.test

public class SomeClass {

    private fun a() {
        object : Any() {
            @java.lang.Deprecated
            val property: Int = 5
        }

    }

}