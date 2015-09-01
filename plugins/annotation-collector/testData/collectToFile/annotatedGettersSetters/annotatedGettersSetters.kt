package org.test

public data class SomeClass {

    public val immutableProperty: Int = 5
        @java.lang.Deprecated get

    public var mutableProperty: String = "String"
        @java.lang.Deprecated get
        @java.lang.Deprecated set

}