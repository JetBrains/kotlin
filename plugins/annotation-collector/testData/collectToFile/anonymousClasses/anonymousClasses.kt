package org.test

public class SomeClass {

    private fun a() {
        object : Any() {
            @Deprecated
            val property: Int = 5
        }

    }

}