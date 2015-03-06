package com.voltvoodoo.saplo4j.model

import java.io.Serializable

public class Language(protected var code: String) : Serializable {

    public fun equals(other: Language): Boolean {
        return other.toString() == this.toString()
    }

    default object {
        public var ENGLISH: Language = Language("en")
        public var SWEDISH: Language = Language("sv")
        private val serialVersionUID = -2442762969929206780
    }
}