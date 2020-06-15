// ERROR: Unresolved reference. None of the following candidates is applicable because of receiver type mismatch:  public inline fun <T> MutableCollection<out Int?>.remove(element: Int?): Boolean defined in kotlin.collections public inline fun <T> MutableList<???>.remove(index: Int): ??? defined in kotlin.collections public inline fun <K, V> MutableMap<out Int?, ???>.remove(key: Int?): ??? defined in kotlin.collections
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
        val s = "aaa$myProp"
    }

    fun test6() {
        myProp!!.compareTo(myProp!!, ignoreCase = true)
    }

    fun test7() {
        val list: List<Int> = ArrayList()
        list.remove(myIntProp)
    }

    fun foo1(s: String) {}
    fun foo2(s: String?) {}
    fun foo3(s: String?) {}
}