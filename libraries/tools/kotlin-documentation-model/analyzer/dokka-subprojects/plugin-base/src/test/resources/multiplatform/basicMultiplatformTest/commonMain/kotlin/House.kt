/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package example

class House(val street: String, val number: Int) {

    /**
     * The owner of the house
     */
    var owner: String = ""

    /**
     * The owner of the house
     */
    val differentOwner: String = ""

    fun addFloor() {}

    class Basement {
        val pickles : List<Any> = mutableListOf()
    }

    companion object {
        val DEFAULT = House("",0)
    }
}
