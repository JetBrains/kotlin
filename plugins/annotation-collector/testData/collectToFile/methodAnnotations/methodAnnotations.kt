package org.test

import javax.inject.*

public class SomeClass {

    Inject
    public fun annotatedFunction() {

        [Named("LocalFunction")]
        fun localFunction() {

        }

    }

}