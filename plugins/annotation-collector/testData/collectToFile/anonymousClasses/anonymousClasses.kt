package org.test

import javax.inject.*

public class SomeClass {

    private fun a() {
        object : Any() {
            [Inject]
            val property: Int = 5
        }

    }

}