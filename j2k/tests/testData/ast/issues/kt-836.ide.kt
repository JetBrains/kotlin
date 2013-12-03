package com.voltvoodoo.saplo4j.model

import java.io.Serializable

public class Language(code: String) : Serializable {
    protected var code: String = 0

    override fun toString(): String {
        return this.code
    }

    {
        this.code = code
    }
}
class Base() {
    fun test() {
    }
    fun toString(): String {
        return "BASE"
    }
}
class Child() : Base() {
    override fun test() {
    }
    override fun toString(): String {
        return "Child"
    }
}