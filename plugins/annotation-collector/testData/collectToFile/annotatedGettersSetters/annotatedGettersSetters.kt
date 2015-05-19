package org.test

import javax.inject.*

public class SomeClass {

    public val immutableProperty: Int = 5
        [Inject] get

    public var mutableProperty: String = "String"
        [Inject] get
        [Inject] set

}