package test

import java.util.ArrayList

class Test {
    private var myProp: String? = null
    private var myIntProp: Int? = null

    fun onCreate() {
        myProp = ""
        myIntProp = 1
    }

    fun test() {
        foo1(myProp!!)
        foo2(myProp!!)
        foo3(myProp)

        myProp!![myIntProp!!]
        println(myProp)

        val b = "aaa" == myProp
        val s = "aaa" + myProp!!

        myProp!!.compareTo(myProp!!, ignoreCase = true)

        val list = ArrayList<Int>()
        list.remove(myIntProp!!)
    }

    fun foo1(s: String) {

    }

    fun foo2(s: String) {

    }

    fun foo3(s: String?) {

    }
}