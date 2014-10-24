// ERROR: 'test' in 'Base' is final and cannot be overridden
// ERROR: This type is final, so it cannot be inherited from
package com.voltvoodoo.saplo4j.model

import java.io.Serializable

public class Language(protected var code: String) : Serializable {

    override fun toString(): String {
        return this.code
    }
}


class Base {
    fun test() {
    }

    override fun toString(): String {
        return "BASE"
    }
}

class Child : Base() {
    override fun test() {
    }

    override fun toString(): String {
        return "Child"
    }
}
