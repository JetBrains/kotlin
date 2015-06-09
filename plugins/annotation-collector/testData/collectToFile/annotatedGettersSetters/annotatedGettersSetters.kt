package org.test

public data class SomeClass {

    public val immutableProperty: Int = 5
        @Deprecated get

    public var mutableProperty: String = "String"
        @Deprecated get
        @Deprecated set

}