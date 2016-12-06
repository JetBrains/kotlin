package com.voltvoodoo.saplo4j.model

import java.io.Serializable

class Language(protected var code: String) : Serializable {

    override fun toString(): String {
        return this.code
    }
}


internal open class Base {
    internal open fun test() {}
    override fun toString(): String {
        return "BASE"
    }
}

internal class Child : Base() {
    override fun test() {}
    override fun toString(): String {
        return "Child"
    }
}
