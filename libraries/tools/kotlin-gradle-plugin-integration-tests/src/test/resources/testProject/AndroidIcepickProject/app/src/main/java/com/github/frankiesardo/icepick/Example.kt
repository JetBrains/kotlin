package com.github.frankiesardo.icepick

import org.parceler.Parcel

@Parcel
class Example {
    lateinit var name: String

    @JvmField
    var age: Int = 0

    constructor() { /*Required empty bean constructor*/
    }

    constructor(age: Int, name: String) {
        this.age = age
        this.name = name
    }
}