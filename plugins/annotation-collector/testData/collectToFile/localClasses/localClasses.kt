package org.test

import javax.inject.*

public class SomeClass {

    public fun someFunction() {
        [Named("LocalKotlinClass")]
        class LocalClass {

            [Inject]
            public var annotatedProperty: String? = null

            [Inject]
            public fun annotatedFunction() {

            }

        }
    }

}