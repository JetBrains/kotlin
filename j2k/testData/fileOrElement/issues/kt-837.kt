package com.voltvoodoo.saplo4j.model

import java.io.Serializable

class Language(protected var code: String) : Serializable {

    fun equals(other: Language): Boolean {
        return other.toString() == this.toString()
    }

    companion object {
        var ENGLISH = Language("en")
        var SWEDISH = Language("sv")
        @JvmStatic private val serialVersionUID = -2442762969929206780L
    }
}