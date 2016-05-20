// ERROR: Type mismatch: inferred type is String? but String was expected
// ERROR: Type mismatch: inferred type is Int? but Int was expected
// ERROR: Type inference failed. Please try to specify type arguments explicitly.
// ERROR: Using 'remove(Int): T' is an error. Use removeAt(index) instead.
package test

import java.util.ArrayList

class Test {
    private var myProp: String? = null
    private var myIntProp: Int? = null

    fun onCreate() {
        myProp = ""
        myIntProp = 1
    }

    fun test1() {
        foo1(myProp!!)
    }

    fun test2() {
        foo2(myProp)
    }

    fun test3() {
        foo3(myProp)
    }

    fun test4() {
        myProp!![myIntProp!!]
        println(myProp)
    }

    fun test5() {
        val b = "aaa" == myProp
        val s = "aaa" + myProp!!
    }

    fun test6() {
        myProp!!.compareTo(myProp!!, ignoreCase = true)
    }

    fun test7() {
        val list = ArrayList<Int>()
        list.remove(myIntProp)
    }

    fun foo1(s: String) {

    }

    fun foo2(s: String) {

    }

    fun foo3(s: String?) {

    }
}